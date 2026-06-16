package ar.utn.donatrack.donaciones.dtos.request;

import ar.utn.donatrack.donaciones.models.donante.TipoPersonaJuridica;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
public class PersonaJuridicaRequestDTO extends PersonaDonanteRequestDTO {

  @NotBlank
  private String razonSocial;

  @NotBlank
  private String rubro;

  @NotBlank
  private TipoPersonaJuridica tipo;

  @Builder.Default
  private List<RepresentanteDTO> representantes = new ArrayList<>();
}