package ar.utn.donatrack.donaciones.dtos.request;

import ar.utn.donatrack.donaciones.models.donante.Genero;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
public class PersonaHumanaRequestDTO extends PersonaDonanteRequestDTO {

  @NotBlank
  private String nombre;

  @NotBlank
  private String apellido;

  @NotBlank
  private LocalDate fechaNacimiento;

  @NotBlank
  private Genero genero;
}