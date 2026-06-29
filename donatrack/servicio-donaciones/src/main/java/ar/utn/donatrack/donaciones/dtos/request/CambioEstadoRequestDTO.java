package ar.utn.donatrack.donaciones.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CambioEstadoRequestDTO {
  @NotBlank
  private String estado;
  private String nombreTransicion;
  private String justificacion;
}