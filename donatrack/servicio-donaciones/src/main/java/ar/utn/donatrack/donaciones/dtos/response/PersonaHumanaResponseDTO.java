package ar.utn.donatrack.donaciones.dtos.response;

import ar.utn.donatrack.donaciones.models.donante.Genero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class PersonaHumanaResponseDTO extends PersonaDonanteResponseDTO {

  private String nombre;
  private String apellido;
  private Integer edad;
  private Genero genero;
}