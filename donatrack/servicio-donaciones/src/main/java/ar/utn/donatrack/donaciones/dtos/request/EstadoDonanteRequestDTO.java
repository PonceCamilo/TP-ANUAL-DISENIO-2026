package ar.utn.donatrack.donaciones.dtos.request;

import ar.utn.donatrack.donaciones.models.donante.EstadoDonante;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EstadoDonanteRequestDTO {

  @NotNull
  private EstadoDonante estado;

  // Opcional a nivel contrato: PersonasValidator la exige SOLO cuando el nuevo
  // estado es BLOQUEADO. Forzar @NotBlank aquí rechazaría transiciones válidas
  // como ACTIVO -> INACTIVO que no requieren justificación.
  private String justificacion;
}
