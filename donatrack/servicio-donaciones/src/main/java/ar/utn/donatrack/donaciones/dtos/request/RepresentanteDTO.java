package ar.utn.donatrack.donaciones.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RepresentanteDTO {

  @NotBlank
  private String nombre;

  @NotBlank
  private String apellido;

  @NotBlank
  @Email
  private String email;
}