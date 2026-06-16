package ar.utn.donatrack.donaciones.dtos.response;

import ar.utn.donatrack.donaciones.models.donante.TipoPersonaJuridica;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class PersonaJuridicaResponseDTO extends PersonaDonanteResponseDTO {

  private String razonSocial;
  private String rubro;
  private TipoPersonaJuridica tipo;
  private List<RepresentanteResponseDTO> representantes;
}