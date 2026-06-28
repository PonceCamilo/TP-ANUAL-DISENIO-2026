package ar.utn.donatrack.donaciones.dtos.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payload que envía el Servicio de Logística cuando la entidad beneficiaria
 * confirma la recepción satisfactoria de una donación.
 */
public record EntregaExitosaCallbackDTO(
        @NotNull UUID idDonacion,
        @NotNull UUID idCamion,
        @NotNull String patenteCamion,
        @NotNull LocalDateTime fechaHoraEntrega
) {}
