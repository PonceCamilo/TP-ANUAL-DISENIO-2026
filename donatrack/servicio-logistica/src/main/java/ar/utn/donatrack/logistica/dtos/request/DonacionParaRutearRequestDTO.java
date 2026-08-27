package ar.utn.donatrack.logistica.dtos.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Representa una donación en estado "Asignación Realizada" tal como la
 * envía quien dispara la planificación. No se reutiliza la clase Donacion
 * de servicio-donaciones: logística solo necesita el id, la entidad destino
 * y la dirección de descarga. El donante no viaja en este contrato.
 */
@Getter
@Setter
@NoArgsConstructor
public class DonacionParaRutearRequestDTO {
    @NotNull
    private UUID idDonacion;
    @NotNull
    private UUID idEntidadBeneficiaria;
    @NotNull
    @Valid
    private DireccionRequestDTO direccionEntrega;
}
