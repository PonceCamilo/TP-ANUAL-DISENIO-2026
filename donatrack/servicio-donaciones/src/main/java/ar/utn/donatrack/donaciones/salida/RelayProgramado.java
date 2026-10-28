package ar.utn.donatrack.donaciones.salida;

import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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

    private final RelayDeMensajes relay;

    /**
     * Con varias instancias, una sola envía a la vez: si dos tomaran el mismo
     * mensaje pendiente, el aviso saldría dos veces. Si la instancia que envía se
     * cae, el bloqueo vence al minuto y otra sigue.
     */
    @Scheduled(fixedDelayString = "${donaciones.salida.intervalo-ms:5000}")
    @SchedulerLock(name = "relay-bandeja-salida", lockAtMostFor = "PT1M")
    public void enviarPendientes() {
        relay.enviarPendientes();
    }

    @Scheduled(cron = "${donaciones.salida.cron-limpieza:0 30 4 * * *}", zone = FechaHoraArgentina.ZONA_ID)
    @SchedulerLock(name = "limpieza-bandeja-salida", lockAtMostFor = "PT10M",
            lockAtLeastFor = "${donaciones.procesos.bloqueo-minimo:PT5M}")
    public void borrarEnviadosAntiguos() {
        relay.borrarEnviadosAntiguos();
    }
}
