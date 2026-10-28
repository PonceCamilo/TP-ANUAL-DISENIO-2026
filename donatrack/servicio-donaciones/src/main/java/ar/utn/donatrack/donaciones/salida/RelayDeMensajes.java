package ar.utn.donatrack.donaciones.salida;

import ar.utn.donatrack.donaciones.config.DonacionesProperties;
import ar.utn.donatrack.donaciones.repositories.jpa.MensajeSalienteJpaRepository;
import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Envía los mensajes de la bandeja de salida y registra cómo le fue a cada uno.
 *
 * Lee un lote de pendientes en una transacción corta, envía cada mensaje fuera
 * de toda transacción (no tiene sentido tener una conexión a la base abierta
 * mientras se espera a un servicio externo) y guarda el resultado de cada envío
 * en su propia transacción. Un mensaje que falla no frena a los demás.
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

    /** Envía los mensajes listos (pendientes cuyo próximo intento ya llegó). Devuelve cuántos salieron. */
    public int enviarPendientes() {
        LocalDateTime ahora = FechaHoraArgentina.ahora();
        List<MensajeSaliente> listos = transaccion.execute(estado -> mensajes.buscarListos(
                EstadoMensaje.PENDIENTE, ahora, PageRequest.of(0, configuracion.tamanioLote())));
        int enviados = 0;
        for (MensajeSaliente mensaje : listos == null ? List.<MensajeSaliente>of() : listos) {
            String error = enviar(mensaje);
            transaccion.executeWithoutResult(estado -> mensajes.findById(mensaje.getId()).ifPresent(actual -> {
                if (error == null) {
                    actual.marcarEnviado(FechaHoraArgentina.ahora());
                } else {
                    actual.registrarFalla(error, FechaHoraArgentina.ahora(), configuracion.maxIntentos());
                }
            }));
            if (error == null) {
                enviados++;
            } else {
                log.warn("[SALIDA] No se pudo enviar el mensaje {} a {} (intento {}): {}",
                        mensaje.getId(), mensaje.getDestino(), mensaje.getIntentos() + 1, error);
            }
        }
        return enviados;
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
