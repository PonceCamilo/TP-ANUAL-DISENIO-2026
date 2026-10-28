package ar.utn.donatrack.donaciones.dtos.response;

import ar.utn.donatrack.donaciones.models.donante.TipoPersonaJuridica;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * Una organización donante. Su tipo (ONG, EMPRESA, ...) sale como
 * tipoOrganizacion, igual que en el alta: "tipo" es el discriminador del JSON
 * (JURIDICA) y antes la clave salía dos veces.
 */
@Getter
@SuperBuilder
public class PersonaJuridicaResponseDTO extends PersonaDonanteResponseDTO {

    private final String razonSocial;
    private final String rubro;
    private final TipoPersonaJuridica tipoOrganizacion;
    private final List<RepresentanteResponseDTO> representantes;
}
