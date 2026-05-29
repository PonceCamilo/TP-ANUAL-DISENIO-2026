package ar.utn.donatrack.donaciones.models.donante;

import ar.utn.donatrack.donaciones.models.contacto.MedioDeContacto;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;


/**
 * Persona donante de tipo humano (persona física).
 * Atributos requeridos: nombre, apellido, edad, número de documento, género y dirección.
 */

@SuperBuilder
@Getter
@Setter
public class PersonaHumanaDonante extends PersonaDonante {

    private String nombre;
    private String apellido;
    private int dni;
    private int edad;
    private Genero genero;
    protected List<MedioDeContacto> contactos;
}
