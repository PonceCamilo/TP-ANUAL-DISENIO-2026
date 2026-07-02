package ar.utn.donatrack.notificaciones.dto.response;

import ar.utn.donatrack.notificaciones.model.EstadoNotificacion;
import ar.utn.donatrack.notificaciones.model.Notificacion;
import ar.utn.donatrack.notificaciones.model.medios.MedioNotificacion;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Vista de salida de una notificación. Evita exponer el modelo de dominio
 * crudo en la API (mismo criterio que los ResponseDTO de servicio-donaciones).
 */
public record NotificacionResponse(
        UUID id,
        String destinatario,
        String mensaje,
        MedioNotificacion  medio,
        //TipoEvento evento,
        EstadoNotificacion estado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaEnvio
) {
    public static NotificacionResponse desde(Notificacion n) {
        return new NotificacionResponse(
                n.getId(),
                n.getDestinatario(),
                n.getMensaje(),
                n.getMedio(),
                //n.getEvento(),
                n.getEstado(),
                n.getFechaCreacion(),
                n.getFechaEnvio()
        );
    }
}
