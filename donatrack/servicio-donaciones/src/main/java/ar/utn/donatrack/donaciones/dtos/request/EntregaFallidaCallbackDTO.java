package ar.utn.donatrack.donaciones.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Payload que envía el Servicio de Logística cuando la entrega no pudo
 * concretarse. Incluye el motivo y si la donación puede ser replanificada.
 */
public record EntregaFallidaCallbackDTO(
        @NotNull UUID idDonacion,
        @NotBlank String motivoFallo,
        boolean replanificable
) {}
