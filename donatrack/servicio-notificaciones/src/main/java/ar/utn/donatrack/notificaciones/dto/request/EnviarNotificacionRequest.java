package ar.utn.donatrack.notificaciones.dto.request;

import ar.utn.donatrack.notificaciones.model.MedioNotificacion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EnviarNotificacionRequest(
        @NotBlank(message = "El destinatario es obligatorio.")
        String destinatario,
        @NotBlank(message = "El mensaje es obligatorio.")
        String mensaje,
        @NotNull(message = "El medio de notificación es obligatorio.")
        MedioNotificacion medio
) {}
