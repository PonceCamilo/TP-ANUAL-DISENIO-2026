package ar.utn.donatrack.donaciones.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
public class RepresentanteRequestDTO {

  @NotBlank
  private String nombre;

  @NotBlank
  private String apellido;

  @NotBlank
  @Email
  private String email;
}