package ar.utn.donatrack.donaciones.models.donante;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
public class PersonaHumana extends PersonaDonante {

    private String nombre;
    private String apellido;
    private int edad;
    private Genero genero;
}
