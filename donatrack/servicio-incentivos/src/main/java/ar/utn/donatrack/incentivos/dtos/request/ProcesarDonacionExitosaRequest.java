package ar.utn.donatrack.incentivos.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Evento enviado por servicio-donaciones cuando una donacion fue entregada")
public record ProcesarDonacionExitosaRequest(
        @Schema(description = "ID del donante", example = "11111111-1111-1111-1111-111111111111")
        @NotNull UUID donanteId,

        @Schema(description = "Contacto del donante para notificaciones", example = "donante@mail.com")
        @NotBlank String destinatario,

        @Schema(description = "Medio de notificacion", example = "EMAIL")
        @NotBlank String medio,

        @Schema(description = "Cantidad de bienes entregados", example = "3")
        int cantidadBienes,

        @Schema(description = "Categorias entregadas", example = "[\"ALIMENTOS\", \"ABRIGO\"]")
        List<String> categoriasDonadas,

        @Schema(description = "Fecha de entrega exitosa", example = "2026-06-24T19:07:54")
        LocalDateTime fecha,

        @Schema(description = "Entidad beneficiaria que recibio la donacion", example = "Comedor Los Pibes")
        String entidadBeneficiaria
) {}
