package ar.utn.donatrack.donaciones.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EstadoDonanteRequestDTO {

  @NotBlank
  private String estado;

  // Opcional a nivel contrato: PersonasValidator la exige SOLO cuando el nuevo
  // estado es BLOQUEADO. Forzar @NotBlank aquí rechazaría transiciones válidas
  // como ACTIVO -> INACTIVO que no requieren justificación.
  private String justificacion;
}
