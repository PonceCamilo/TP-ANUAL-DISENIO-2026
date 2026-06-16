package ar.utn.donatrack.donaciones.dtos.request;

import ar.utn.donatrack.donaciones.models.donante.TipoPersonaJuridica;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
public class PersonaJuridicaRequestDTO extends PersonaDonanteRequestDTO {

  @NotBlank
  private String razonSocial;

  @NotBlank
  private String rubro;

  @NotNull
  private TipoPersonaJuridica tipo;

  @Valid
  @NotEmpty
  @lombok.Builder.Default
  private List<RepresentanteRequestDTO> representantes = new ArrayList<>();
}