package ar.utn.donatrack.donaciones.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class BienRequestDTO {
  @NotBlank
  private String subcategoria;
  private String descripcion;
  private String foto;
  @Positive
  private int cantidad;
  @NotBlank
  private String unidad;
  private Boolean esNuevo;
  private LocalDate fechaVencimiento;
}
