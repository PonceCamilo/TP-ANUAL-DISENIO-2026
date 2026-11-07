package ar.utn.donatrack.donaciones.salida;

import ar.utn.donatrack.donaciones.config.DonacionesProperties;
import ar.utn.donatrack.donaciones.repositories.jpa.MensajeSalienteJpaRepository;
import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Envía los mensajes de la bandeja de salida y registra cómo le fue a cada uno.
 *
 * Lee los pendientes en transacciones cortas, envía cada mensaje fuera de toda
 * transacción (no tiene sentido tener una conexión a la base abierta mientras se
 * espera a un servicio externo) y guarda el resultado de cada envío en su propia
 * transacción.
 *
 * CADA DESTINO EN SU PROPIO HILO. En cada corrida, los pendientes de cada
 * destino se envían en un hilo aparte, de a lotes (donaciones.salida.tamanio-lote)
 * y en orden de llegada, hasta terminarlos o hasta el tiempo máximo de la
 * corrida. Si un envío falla, los demás mensajes de ese destino esperan a la
 * corrida siguiente; los de los otros destinos siguen. Antes se tomaban los 50
 * más viejos sin mirar el destino y se enviaban de a uno: con Incentivos lento y
 * cientos de avisos suyos atrasados, los avisos a Notificaciones quedaban detrás
 * de todos, y cada aviso a un servicio colgado costaba los 7 s de timeout aunque
 * el anterior ya hubiera fallado. Y un destino rápido no pasaba de 50 avisos cada
 * 5 s aunque tuviera miles pendientes.
 *
 * El tiempo máximo existe porque una corrida no puede durar más que el bloqueo
 * que evita que dos instancias envíen a la vez (RelayProgramado): al llegar,
 * cada hilo deja de tomar mensajes y lo que falta sale en la corrida siguiente.
 *
 * Lo dispara RelayProgramado cada pocos segundos.
 */
@Slf4j
@Component
public class RelayDeMensajes {

    private final MensajeSalienteJpaRepository mensajes;
    private final Map<String, EnviadorDeMensajes> enviadores;
    private final TransactionOperations transaccion;
    private final DonacionesProperties.Salida configuracion;

    public RelayDeMensajes(MensajeSalienteJpaRepository mensajes, List<EnviadorDeMensajes> enviadores,
                           TransactionOperations transaccion, DonacionesProperties propiedades) {
        Map<String, EnviadorDeMensajes> porDestino = new HashMap<>();
        for (EnviadorDeMensajes enviador : enviadores) {
            for (String destino : enviador.destinos()) {
                if (porDestino.putIfAbsent(destino, enviador) != null) {
                    throw new IllegalStateException("Hay dos enviadores para el destino " + destino);
                }
            }
        }
        this.mensajes = mensajes;
        this.enviadores = Map.copyOf(porDestino);
        this.transaccion = transaccion;
        this.configuracion = propiedades.salida();
    }

    /**
     * Envía los mensajes listos (pendientes cuyo próximo intento ya llegó) de
     * todos los destinos, cada uno en su hilo, hasta terminarlos o hasta que pase
     * el tiempo máximo. Vuelve cuando terminaron todos los hilos. Los que no
     * llegan a salir, y los que siguen a una falla en su mismo destino, quedan
     * pendientes sin contar como intento. Devuelve cuántos salieron.
     */
    public int enviarPendientes(Duration tiempoMaximo) {
        Instant corte = FechaHoraArgentina.reloj().instant().plus(tiempoMaximo);
        LocalDateTime ahora = FechaHoraArgentina.ahora();
        List<String> destinos = transaccion.execute(estado ->
                mensajes.buscarDestinosConListos(EstadoMensaje.PENDIENTE, ahora));
        if (destinos == null || destinos.isEmpty()) {
            return 0;
        }
        Map<String, Future<Integer>> envios = new LinkedHashMap<>();
        // Al cerrarse, el ejecutor espera a que terminen todos los hilos.
        try (ExecutorService hilos = Executors.newThreadPerTaskExecutor(Thread.ofPlatform().name("relay-", 1).factory())) {
            for (String destino : destinos) {
                envios.put(destino, hilos.submit(() -> enviarDestino(destino, ahora, corte, tiempoMaximo)));
            }
        }
        int enviados = 0;
        for (Map.Entry<String, Future<Integer>> envio : envios.entrySet()) {
            enviados += enviadosA(envio.getKey(), envio.getValue());
        }
        return enviados;
    }

    /**
     * Envía los listos de un destino, de a lotes y en orden de llegada, hasta
     * terminarlos, llegar al corte o que uno falle.
     */
    private int enviarDestino(String destino, LocalDateTime ahora, Instant corte, Duration tiempoMaximo) {
        int tamanio = configuracion.tamanioLote();
        int enviados = 0;
        List<MensajeSaliente> lote;
        do {
            lote = transaccion.execute(estado ->
                    mensajes.buscarListos(EstadoMensaje.PENDIENTE, destino, ahora, PageRequest.of(0, tamanio)));
            if (lote == null) {
                return enviados;
            }
            for (MensajeSaliente mensaje : lote) {
                if (!FechaHoraArgentina.reloj().instant().isBefore(corte)) {
                    log.info("[SALIDA] {}: la corrida llegó a su tiempo máximo ({}); los pendientes salen en la siguiente.",
                            destino, tiempoMaximo);
                    return enviados;
                }
                String error = enviar(mensaje);
                registrar(mensaje, error);
                if (error != null) {
                    log.warn("[SALIDA] No se pudo enviar el mensaje {} a {} (intento {}): {}. Los demás de ese destino "
                            + "esperan a la corrida siguiente.", mensaje.getId(), destino, mensaje.getIntentos() + 1, error);
                    return enviados;
                }
                enviados++;
            }
        } while (lote.size() == tamanio);
        return enviados;
    }

    private void registrar(MensajeSaliente mensaje, String error) {
        transaccion.executeWithoutResult(estado -> mensajes.findById(mensaje.getId()).ifPresent(actual -> {
            if (error == null) {
                actual.marcarEnviado(FechaHoraArgentina.ahora());
            } else {
                actual.registrarFalla(error, FechaHoraArgentina.ahora(), configuracion.maxIntentos());
            }
        }));
    }

    private static int enviadosA(String destino, Future<Integer> envio) {
        try {
            return envio.get();
        } catch (ExecutionException e) {
            log.error("[SALIDA] Se cortó el envío a {}: {}", destino, e.getCause().getMessage());
            return 0;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        }
    }

    /** Borra los mensajes enviados hace más de los días de retención. */
    public int borrarEnviadosAntiguos() {
        LocalDateTime limite = FechaHoraArgentina.ahora().minusDays(configuracion.diasRetencion());
        Integer borrados = transaccion.execute(estado -> mensajes.borrarEnviadosAntesDe(EstadoMensaje.ENVIADO, limite));
        return borrados == null ? 0 : borrados;
    }

    private String enviar(MensajeSaliente mensaje) {
        EnviadorDeMensajes enviador = enviadores.get(mensaje.getDestino());
        if (enviador == null) {
            return "No hay quién envíe mensajes a " + mensaje.getDestino();
        }
        try {
            enviador.enviar(mensaje.getDestino(), mensaje.getPayload());
            return null;
        } catch (Exception e) {
            return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        }
    }
}
