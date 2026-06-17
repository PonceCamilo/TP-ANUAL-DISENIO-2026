package ar.utn.donatrack.incentivos.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Request que envía servicio-donaciones cuando un donante realiza una donación.
 * Contiene la info necesaria para actualizar métricas y notificar al donante.
 */
public record ProcesarDonacionRequest(
        @NotNull UUID donanteId,
        @NotBlank String destinatario,   // email o número del donante
        @NotBlank String medio,          // "EMAIL", "SMS" o "WHATSAPP"
        int cantidadBienes,
        List<String> categoriasDonadas
) {}
