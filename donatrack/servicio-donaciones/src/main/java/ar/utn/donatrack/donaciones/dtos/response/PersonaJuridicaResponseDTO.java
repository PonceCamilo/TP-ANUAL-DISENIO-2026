package ar.utn.donatrack.donaciones.dtos.response;

import ar.utn.donatrack.donaciones.models.donante.TipoPersonaJuridica;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@SuperBuilder
public class PersonaJuridicaResponseDTO extends PersonaDonanteResponseDTO {

    private final String razonSocial;
    private final String rubro;
    private final TipoPersonaJuridica tipo;
    private final List<RepresentanteResponseDTO> representantes;
}
