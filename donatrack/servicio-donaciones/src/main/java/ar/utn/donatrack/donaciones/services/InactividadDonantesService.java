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

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tarea calendarizada que corre todos los días a medianoche.
 * Busca donantes sin interacción en más de 20 días y les envía
 * una notificación incentivándolos a realizar una nueva donación
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
        LocalDateTime fechaLimite = FechaHoraArgentina.ahora().minusDays(propiedades.inactividad().dias());

        // El filtro lo hace la base con el índice de ultima_interaccion
        // (Entrega 5): antes se cargaban todos los donantes y se filtraba acá.
        List<PersonaDonante> inactivos = donanteRepository.obtenerInactivasDesde(fechaLimite);

        log.info("[INACTIVIDAD] {} donantes inactivos detectados", inactivos.size());

        for (PersonaDonante donante : inactivos) {
            notificarDonante(donante);
        }
    }

    private void notificarDonante(PersonaDonante donante) {
        MedioDeContacto medio = donante.getMedioContactoPredeterminado();
        if (medio == null) {
            log.warn("[INACTIVIDAD] Donante {} sin medio de contacto predeterminado, se omite", donante.getId());
            return;
        }

        String tipoMedio = mapearTipoMedio(medio);
        String mensaje = "¡Hace más de " + propiedades.inactividad().dias() + " días que no realizas una donación! "
                + "Tu ayuda hace la diferencia. Ingresá a DonaTrack y sumá tu aporte!!.";

        notificacionClient.enviarNotificacion(medio.getValor(), mensaje, tipoMedio);
    }

    private String mapearTipoMedio(MedioDeContacto medio) {
        if (medio instanceof Email) return "EMAIL";
        if (medio instanceof Telefono) return "SMS";
        if (medio instanceof Whatsapp) return "WHATSAPP";
        return "EMAIL";
    }
}