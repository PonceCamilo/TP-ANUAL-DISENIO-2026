package ar.utn.donatrack.donaciones.models.donante;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@SuperBuilder
@Getter
@Setter
public class PersonaHumana extends PersonaDonante {

    private String nombre;
    private String apellido;
    private LocalDate fechaNacimiento;
    private Genero genero;
}
