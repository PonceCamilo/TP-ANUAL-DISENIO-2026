package ar.utn.donatrack.donaciones.dtos.request;

import ar.utn.donatrack.donaciones.models.entidad.Localidad;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DireccionDTO {

  @NotBlank
  private String calle;

  @NotBlank
  private int numero;

  @NotBlank
  private Localidad localidad;

  @NotBlank
  private String codigoPostal;
}
