package ar.utn.donatrack.incentivos.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Evento enviado por servicio-donaciones cuando una donacion fue entregada")
public record ProcesarDonacionExitosaRequest(
        @Schema(description = "ID del donante", example = "11111111-1111-1111-1111-111111111111")
        @NotNull UUID donanteId,

        @Schema(description = "Contacto del donante para notificaciones", example = "donante@mail.com")
        @NotBlank String destinatario,

        @Schema(description = "Medio de notificacion", example = "EMAIL")
        @NotBlank String medio
) {}
