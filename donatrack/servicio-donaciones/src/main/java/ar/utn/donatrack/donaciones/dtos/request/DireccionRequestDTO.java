package ar.utn.donatrack.donaciones.dtos.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class DireccionRequestDTO {

  @NotBlank
  private String calle;

  @Positive
  private int numero;

  @NotBlank
  private String codigoPostal;

  @Valid
  @NotNull
  private LocalidadRequestDTO localidad;
}