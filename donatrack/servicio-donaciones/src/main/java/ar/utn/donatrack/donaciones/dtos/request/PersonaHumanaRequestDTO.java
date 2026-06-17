package ar.utn.donatrack.donaciones.dtos.request;

import ar.utn.donatrack.donaciones.models.donante.Genero;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonaHumanaRequestDTO extends PersonaDonanteRequestDTO {

  @NotBlank
  private String nombre;

  @NotBlank
  private String apellido;

  @NotNull
  private LocalDate fechaNacimiento;

  @NotNull
  private Genero genero;
}