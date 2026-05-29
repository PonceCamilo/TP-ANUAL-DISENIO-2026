package ar.utn.donatrack.donaciones.models.donante;

import ar.utn.donatrack.donaciones.models.contacto.MedioDeContacto;
import java.util.List;
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
    private List<MedioDeContacto> contactos;
}
