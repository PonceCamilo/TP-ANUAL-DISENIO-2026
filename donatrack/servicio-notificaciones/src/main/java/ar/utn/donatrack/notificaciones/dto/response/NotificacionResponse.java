package ar.utn.donatrack.notificaciones.dto.response;

import ar.utn.donatrack.notificaciones.model.EstadoNotificacion;
import ar.utn.donatrack.notificaciones.model.MedioNotificacion;
import ar.utn.donatrack.notificaciones.model.Notificacion;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificacionResponse(
        UUID id,
        String destinatario,
        String mensaje,
        MedioNotificacion medio,
        EstadoNotificacion estado,
        LocalDateTime creadaEn,
        LocalDateTime enviadaEn
) {
    public static NotificacionResponse desde(Notificacion n) {
        return new NotificacionResponse(
                n.getId(), n.getDestinatario(), n.getMensaje(),
                n.getMedio(), n.getEstado(), n.getCreadaEn(), n.getEnviadaEn()
        );
    }
}
