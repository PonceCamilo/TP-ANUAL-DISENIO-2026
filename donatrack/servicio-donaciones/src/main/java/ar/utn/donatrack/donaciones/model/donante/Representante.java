package ar.utn.donatrack.donaciones.model.donante;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Persona física habilitada para operar en nombre de una persona jurídica.
 * Una persona jurídica puede tener múltiples representantes.
 */

@Builder
@Getter
@Setter
public class Representante {

    private final String nombre;
    private final String apellido;
    private final String email;
    private String telefono; // opcional
}
