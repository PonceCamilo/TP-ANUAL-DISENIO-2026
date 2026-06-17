package ar.utn.donatrack.notificaciones.services;

import ar.utn.donatrack.notificaciones.dto.SolicitudNotificacionDto;
import ar.utn.donatrack.notificaciones.interfaces.repositories.NotificacionRepositoryInterface;
import ar.utn.donatrack.notificaciones.interfaces.services.EstrategiaEnvioInterface;
import ar.utn.donatrack.notificaciones.interfaces.services.NotificacionServiceInterface;
import ar.utn.donatrack.notificaciones.model.EstadoNotificacion;
import ar.utn.donatrack.notificaciones.model.Notificacion;
import ar.utn.donatrack.notificaciones.model.TipoMedioNotificacion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static ar.utn.donatrack.notificaciones.model.TipoMedioNotificacion.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionService implements NotificacionServiceInterface {

    private final NotificacionRepositoryInterface repositorio;
    private final EnvioEmailService envioEmailService;
    private final EnvioSmsService envioSmsService;
    private final EnvioWhatsappService envioWhatsappService;

    private Map<TipoMedioNotificacion, EstrategiaEnvioInterface> estrategias;

    private Map<TipoMedioNotificacion, EstrategiaEnvioInterface> obtenerEstrategias() {
        if (estrategias == null) {
            estrategias = Map.of(
                    EMAIL, envioEmailService,
                    SMS, envioSmsService,
                    WHATSAPP, envioWhatsappService
            );
        }
        return estrategias;
    }

    @Override
    public void enviar(SolicitudNotificacionDto solicitud) {
        Notificacion notificacion = Notificacion.builder()
                .id(UUID.randomUUID())
                .destinatario(solicitud.destinatario())
                .mensaje(solicitud.mensaje())
                .medio(solicitud.medio())
                .evento(solicitud.evento())
                .estado(EstadoNotificacion.PENDIENTE)
                .fechaCreacion(LocalDateTime.now())
                .build();

        repositorio.guardar(notificacion);

        try {
            EstrategiaEnvioInterface estrategia = obtenerEstrategias().get(notificacion.getMedio());
            estrategia.enviar(notificacion);
            notificacion.setEstado(EstadoNotificacion.ENVIADA);
            notificacion.setFechaEnvio(LocalDateTime.now());
        } catch (Exception e) {
            notificacion.setEstado(EstadoNotificacion.FALLIDA);
            log.error("[NOTIFICACION] Falló el envío a {}: {}",
                    notificacion.getDestinatario(), e.getMessage());
        }

        repositorio.guardar(notificacion);
    }
}