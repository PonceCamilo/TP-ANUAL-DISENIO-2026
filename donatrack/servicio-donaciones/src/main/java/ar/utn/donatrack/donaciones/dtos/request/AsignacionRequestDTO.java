package ar.utn.donatrack.donaciones.dtos.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Confirma la entidad beneficiaria destino de una donación que está EN_DEPOSITO.
 */
@Getter
@Setter
@NoArgsConstructor
public class AsignacionRequestDTO {

  @NotNull
  private UUID idEntidadBeneficiaria;
}
