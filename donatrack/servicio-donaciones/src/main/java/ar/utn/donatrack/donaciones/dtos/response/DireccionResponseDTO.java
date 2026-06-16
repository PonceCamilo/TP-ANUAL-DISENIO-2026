package ar.utn.donatrack.donaciones.dtos.response;

import ar.utn.donatrack.donaciones.models.entidad.Localidad;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DireccionResponseDTO {

  private String calle;
  private int numero;
  private Localidad localidad;
  private String codigoPostal;
}
