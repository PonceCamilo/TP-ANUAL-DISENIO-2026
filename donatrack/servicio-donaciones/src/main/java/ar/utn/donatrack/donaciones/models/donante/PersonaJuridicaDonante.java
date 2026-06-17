package ar.utn.donatrack.donaciones.models.donante;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * Persona donante de tipo jurídico (empresa, ONG, institución, etc.).
 * Debe contar con al menos un representante habilitado para operar.
 */

@SuperBuilder
@Getter
@Setter
public class PersonaJuridicaDonante extends PersonaDonante {

    private String razonSocial;
    private TipoPersonaJuridica tipo;
    private String rubro;
    private List<Representante> representantes;
}
