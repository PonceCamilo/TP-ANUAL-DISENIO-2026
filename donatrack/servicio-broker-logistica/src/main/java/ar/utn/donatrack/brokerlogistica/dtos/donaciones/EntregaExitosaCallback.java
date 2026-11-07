package ar.utn.donatrack.brokerlogistica.dtos.donaciones;

import java.time.LocalDateTime;
import java.util.UUID;

/** Mismo contrato que EntregaExitosaCallbackDTO de servicio-donaciones. */
public record EntregaExitosaCallback(UUID idDonacion, UUID idCamion, String patenteCamion, LocalDateTime fechaHoraEntrega) {
}
