package ar.utn.donatrack.notificaciones.dto.response;

import ar.utn.donatrack.notificaciones.model.EstadoNotificacion;
import ar.utn.donatrack.notificaciones.model.Notificacion;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Vista de salida de una notificación. Evita exponer el modelo de dominio
 * crudo en la API (mismo criterio que los ResponseDTO de servicio-donaciones).
 * `medio` se expone como String (el nombre del medio) para no filtrar la
 * jerarquía MedioNotificacion en la API.
 */
public record NotificacionResponse(
        UUID id,
        String destinatario,
        String mensaje,
        String medio,
        EstadoNotificacion estado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaEnvio
) {
    public static NotificacionResponse desde(Notificacion n) {
        return new NotificacionResponse(
                n.getId(),
                n.getDestinatario(),
                n.getMensaje(),
                n.getMedio() != null ? n.getMedio().getNombre() : null,
                n.getEstado(),
                n.getFechaCreacion(),
                n.getFechaEnvio()
        );
    }
}
