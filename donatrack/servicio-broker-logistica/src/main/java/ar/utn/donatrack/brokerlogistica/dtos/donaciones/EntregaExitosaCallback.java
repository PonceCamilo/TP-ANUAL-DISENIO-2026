package ar.utn.donatrack.brokerlogistica.dtos.donaciones;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Mismo contrato que EntregaExitosaCallbackDTO de servicio-donaciones, con las
 * fotos de la recepción (Entrega 6). El proveedor externo no manda fotos: para él
 * se usa el constructor sin ellas.
 */
public record EntregaExitosaCallback(UUID idDonacion, UUID idCamion, String patenteCamion,
                                     LocalDateTime fechaHoraEntrega, List<String> fotos) {

    public EntregaExitosaCallback(UUID idDonacion, UUID idCamion, String patenteCamion, LocalDateTime fechaHoraEntrega) {
        this(idDonacion, idCamion, patenteCamion, fechaHoraEntrega, List.of());
    }
}
