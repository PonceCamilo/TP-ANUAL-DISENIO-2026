package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.notificaciones.Notificador;
import ar.utn.donatrack.donaciones.config.DonacionesProperties;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioPersonasDonantes;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import java.util.UUID;
import java.util.Set;
import java.util.HashSet;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Tarea calendarizada que corre todos los días a medianoche.
 *
 * Avisa a las personas donantes que no interactúan con la plataforma desde hace
 * más de 20 días (donaciones.inactividad.dias), como pide el enunciado, para
 * incentivarlas a donar otra vez. Registrar una donación cuenta como interacción.
 *
 * A cada persona se le avisa una vez por período de inactividad, no todas las
 * noches, y nunca a una persona BLOQUEADA.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InactividadDonantesService {

    private final RepositorioPersonasDonantes donanteRepository;
    private final Notificador notificador;
    private final DonacionesProperties propiedades;
    private final TransactionOperations transaccion;

    /**
     * Se procesa por lotes: cada lote se marca como avisado y deja de cumplir la
     * consulta, así que siempre se pide el primero. Antes se cargaban todos los
     * inactivos de una vez y se los volvía a avisar todas las noches.
     *
     * Cada lote es una transacción corta, como en el matchmaking y el despacho: los
     * avisos del lote y su marca de avisado se confirman juntos. Antes la tarea
     * entera era una sola transacción: a los 20 días de importar el CSV, las
     * 20.000 personas quedaban inactivas a la vez, sus filas quedaban bloqueadas
     * hasta el final (una persona que donaba en ese momento esperaba) y un error en
     * el último lote deshacía todos los avisos.
     */
    @Scheduled(cron = "${donaciones.inactividad.cron:0 0 0 * * *}", zone = FechaHoraArgentina.ZONA_ID)
    @SchedulerLock(name = "aviso-inactividad", lockAtMostFor = "PT1H",
            lockAtLeastFor = "${donaciones.procesos.bloqueo-minimo:PT5M}")
    public void notificarDonantesInactivos() {
        LocalDateTime fechaLimite = FechaHoraArgentina.ahora().minusDays(propiedades.inactividad().dias());
        Set<UUID> procesados = new HashSet<>();
        Integer avisados;
        do {
            avisados = transaccion.execute(estado -> avisarLote(fechaLimite, procesados));
        } while (avisados != null && avisados > 0);

        log.info("[INACTIVIDAD] {} donantes inactivos avisados", procesados.size());
    }

    /** Avisa al lote siguiente y lo marca como avisado. Devuelve a cuántos avisó: 0 si no queda nadie. */
    private int avisarLote(LocalDateTime fechaLimite, Set<UUID> procesados) {
        List<PersonaDonante> lote = donanteRepository.obtenerParaAvisoDeInactividad(
                fechaLimite, propiedades.inactividad().tamanioLote());
        List<PersonaDonante> nuevos = lote.stream().filter(persona -> procesados.add(persona.getId())).toList();
        if (nuevos.isEmpty()) {
            if (!lote.isEmpty()) {
                // Salvaguarda: si el aviso no quedó registrado, no repetir el mismo lote para siempre.
                log.error("[INACTIVIDAD] El último lote no registró el aviso; se corta la tarea para no repetirlo.");
            }
            return 0;
        }
        nuevos.forEach(this::notificarDonante);
        donanteRepository.registrarAvisoDeInactividad(
                nuevos.stream().map(PersonaDonante::getId).toList(), FechaHoraArgentina.ahora());
        return nuevos.size();
    }

    private void notificarDonante(PersonaDonante donante) {
        if (!notificador.inactividad(donante)) {
            log.warn("[INACTIVIDAD] Donante {} sin ningún canal de contacto, se omite", donante.getId());
        }
    }
}
