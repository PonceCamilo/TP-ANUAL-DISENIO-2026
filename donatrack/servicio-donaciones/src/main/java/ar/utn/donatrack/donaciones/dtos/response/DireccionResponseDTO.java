package ar.utn.donatrack.donaciones.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DireccionResponseDTO {

  private String calle;
  private int numero;
  private String codigoPostal;
  private LocalidadResponseDTO localidad;
}