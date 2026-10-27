package ar.utn.donatrack.donaciones.services;

import org.springframework.transaction.annotation.Transactional;
import ar.utn.donatrack.donaciones.notificaciones.Notificador;
import ar.utn.donatrack.donaciones.config.DonacionesProperties;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioPersonasDonantes;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

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

    @Scheduled(cron = "${donaciones.inactividad.cron:0 0 0 * * *}", zone = FechaHoraArgentina.ZONA_ID)
    @Transactional
    public void notificarDonantesInactivos() {
        int dias = propiedades.inactividad().dias();
        LocalDateTime fechaLimite = FechaHoraArgentina.ahora().minusDays(dias);

        // Se procesa por lotes: cada lote se marca como avisado y deja de cumplir
        // la consulta, así que siempre se pide el primero. Antes se cargaban todos
        // los inactivos de una vez y se los volvía a avisar todas las noches.
        Set<UUID> procesados = new HashSet<>();
        List<PersonaDonante> lote;
        while (!(lote = donanteRepository.obtenerParaAvisoDeInactividad(
                fechaLimite, propiedades.inactividad().tamanioLote())).isEmpty()) {
            List<PersonaDonante> nuevos = lote.stream().filter(persona -> procesados.add(persona.getId())).toList();
            if (nuevos.isEmpty()) {
                // Salvaguarda: si el aviso no quedó registrado, no repetir el mismo lote para siempre.
                log.error("[INACTIVIDAD] El último lote no registró el aviso; se corta la tarea para no repetirlo.");
                break;
            }
            nuevos.forEach(this::notificarDonante);
            donanteRepository.registrarAvisoDeInactividad(
                    nuevos.stream().map(PersonaDonante::getId).toList(), FechaHoraArgentina.ahora());
        }

        log.info("[INACTIVIDAD] {} donantes inactivos avisados", procesados.size());
    }

    private void notificarDonante(PersonaDonante donante) {
        if (!notificador.inactividad(donante)) {
            log.warn("[INACTIVIDAD] Donante {} sin ningún canal de contacto, se omite", donante.getId());
        }
    }
}
