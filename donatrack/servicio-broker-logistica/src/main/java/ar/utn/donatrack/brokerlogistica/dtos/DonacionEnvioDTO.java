package ar.utn.donatrack.brokerlogistica.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Una donación en estado ASIGNACION_REALIZADA, en el contrato neutral del
 * broker. nombreEntidad es opcional: lo usan los proveedores que necesitan un
 * destinatario legible.
 */
public record DonacionEnvioDTO(
        @NotNull UUID idDonacion,
        @NotNull UUID idEntidadBeneficiaria,
        String nombreEntidad,
        @NotNull @Valid DireccionDTO direccionEntrega,
        // Entrega 6: qué lleva, para que logística lo muestre al chofer y a la administración.
        String resumen) {

    public DonacionEnvioDTO(UUID idDonacion, UUID idEntidadBeneficiaria, String nombreEntidad,
                            DireccionDTO direccionEntrega) {
        this(idDonacion, idEntidadBeneficiaria, nombreEntidad, direccionEntrega, null);
    }
}
