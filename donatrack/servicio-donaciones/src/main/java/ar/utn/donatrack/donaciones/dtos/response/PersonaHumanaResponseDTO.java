package ar.utn.donatrack.donaciones.dtos.response;

import ar.utn.donatrack.donaciones.models.donante.Genero;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class PersonaHumanaResponseDTO extends PersonaDonanteResponseDTO {
  private String nombre;
  private String apellido;
  private int edad;
  private Genero genero;
}