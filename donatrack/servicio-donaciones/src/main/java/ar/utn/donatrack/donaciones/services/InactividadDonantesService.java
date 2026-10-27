package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.clientes.NotificacionClient;
import ar.utn.donatrack.donaciones.config.DonacionesProperties;
import ar.utn.donatrack.donaciones.interfaces.repositories.PersonaDonanteRepositoryInterface;
import ar.utn.donatrack.donaciones.models.contacto.Email;
import ar.utn.donatrack.donaciones.models.contacto.MedioDeContacto;
import ar.utn.donatrack.donaciones.models.contacto.Telefono;
import ar.utn.donatrack.donaciones.models.contacto.Whatsapp;
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

    private final PersonaDonanteRepositoryInterface donanteRepository;
    private final NotificacionClient notificacionClient;
    private final DonacionesProperties propiedades;

    @Scheduled(cron = "${donaciones.inactividad.cron:0 0 0 * * *}", zone = FechaHoraArgentina.ZONA_ID)
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
        MedioDeContacto medio = donante.getMedioContactoPredeterminado();
        if (medio == null) {
            log.warn("[INACTIVIDAD] Donante {} sin medio de contacto predeterminado, se omite", donante.getId());
            return;
        }

        String tipoMedio = mapearTipoMedio(medio);
        String mensaje = "¡Hace más de " + propiedades.inactividad().dias() + " días que no tenemos noticias tuyas! "
                + "Tu ayuda hace la diferencia: ingresá a DonaTrack y sumá una nueva donación.";

        notificacionClient.enviarNotificacion(medio.getValor(), mensaje, tipoMedio);
    }

    private String mapearTipoMedio(MedioDeContacto medio) {
        if (medio instanceof Email) return "EMAIL";
        if (medio instanceof Telefono) return "SMS";
        if (medio instanceof Whatsapp) return "WHATSAPP";
        return "EMAIL";
    }
}