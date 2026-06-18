package ar.utn.donatrack.donaciones.models.donante;

import ar.utn.donatrack.donaciones.models.contacto.MedioDeContacto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Persona física habilitada para operar en nombre de una PersonaJuridicaDonante.
 * El email es campo directo para facilitar búsqueda/remoción sin recorrer la lista.
 */

@Builder
@Getter
@Setter
public class Representante {

    private String nombre;
    private String apellido;
    private String email;

    @Builder.Default
    private List<MedioDeContacto> contactos = new ArrayList<>();
}
