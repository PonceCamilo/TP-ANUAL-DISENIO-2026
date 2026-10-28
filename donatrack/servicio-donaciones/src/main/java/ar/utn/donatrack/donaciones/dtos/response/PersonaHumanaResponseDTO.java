package ar.utn.donatrack.donaciones.dtos.response;

import ar.utn.donatrack.donaciones.models.donante.Genero;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class PersonaHumanaResponseDTO extends PersonaDonanteResponseDTO {

    private final String nombre;
    private final String apellido;
    private final Integer edad;
    private final Genero genero;
}
