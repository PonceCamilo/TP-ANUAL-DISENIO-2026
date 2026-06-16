package ar.utn.donatrack.donaciones.dtos.request;

import ar.utn.donatrack.donaciones.models.donante.EstadoDonante;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EstadoDonanteRequestDTO {

  @NotNull
  private EstadoDonante estado;

  @NotBlank
  private String justificacion;
}
