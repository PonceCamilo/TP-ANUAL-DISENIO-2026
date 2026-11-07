package ar.utn.donatrack.donaciones.salida;

import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Programa el relay de la bandeja de salida: envía cada pocos segundos y limpia
 * de madrugada los mensajes ya enviados. Se puede apagar con
 * donaciones.salida.habilitada=false (los tests lo apagan para medir consultas
 * sin un proceso de fondo).
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "donaciones.salida.habilitada", havingValue = "true", matchIfMissing = true)
public class RelayProgramado {

    /**
     * Lo más que dura el bloqueo del relay: si la instancia que envía se cae, otra
     * sigue a lo sumo un minuto después.
     */
    static final String BLOQUEO_MAXIMO = "PT1M";

    /**
     * Hasta cuándo toma mensajes una corrida: la mitad del bloqueo. El mensaje que
     * se está enviando al llegar al corte termina igual, y con los timeouts (2 s
     * de conexión y 5 s de respuesta o de confirmación del broker) entra holgado
     * en la otra mitad. Así una corrida nunca dura más que su bloqueo.
     */
    static final Duration CORTE = Duration.parse(BLOQUEO_MAXIMO).dividedBy(2);

    private final RelayDeMensajes relay;

    /**
     * Con varias instancias, una sola envía a la vez: si dos tomaran el mismo
     * mensaje pendiente, el aviso saldría dos veces.
     *
     * Sin el corte, un servicio lento estiraba la corrida: 50 mensajes que tardan
     * un par de segundos cada uno pasan del minuto, el bloqueo vencía con la
     * corrida a medias y otra instancia empezaba a enviar los mismos pendientes. Y
     * al terminar, la primera liberaba el bloqueo que ya era de la segunda (se
     * libera por nombre de tarea), así que una tercera corrida podía arrancar
     * junto a la segunda.
     */
    @Scheduled(fixedDelayString = "${donaciones.salida.intervalo-ms:5000}")
    @SchedulerLock(name = "relay-bandeja-salida", lockAtMostFor = BLOQUEO_MAXIMO)
    public void enviarPendientes() {
        relay.enviarPendientes(CORTE);
    }

    @Scheduled(cron = "${donaciones.salida.cron-limpieza:0 30 4 * * *}", zone = FechaHoraArgentina.ZONA_ID)
    @SchedulerLock(name = "limpieza-bandeja-salida", lockAtMostFor = "PT10M",
            lockAtLeastFor = "${donaciones.procesos.bloqueo-minimo:PT5M}")
    public void borrarEnviadosAntiguos() {
        relay.borrarEnviadosAntiguos();
    }
}
