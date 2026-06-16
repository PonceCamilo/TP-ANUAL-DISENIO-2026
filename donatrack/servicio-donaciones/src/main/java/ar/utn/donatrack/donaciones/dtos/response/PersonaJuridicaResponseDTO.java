package ar.utn.donatrack.donaciones.dtos.response;

import ar.utn.donatrack.donaciones.dtos.request.RepresentanteDTO;
import ar.utn.donatrack.donaciones.models.donante.TipoPersonaJuridica;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@SuperBuilder
public class PersonaJuridicaResponseDTO extends PersonaDonanteResponseDTO {
  private String razonSocial;
  private List<RepresentanteDTO> representantes;
  private String rubro;
  private TipoPersonaJuridica tipo;
}