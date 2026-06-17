package ar.utn.donatrack.donaciones.dtos.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocalidadRequestDTO {

  @NotBlank
  private String nombre;

  @Valid
  @NotNull
  private ProvinciaRequestDTO provincia;
}