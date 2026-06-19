package ar.utn.donatrack.donaciones.dtos.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class BienResponseDTO {
  private String subcategoria;
  private String descripcion;
  private String foto;
  private int cantidad;
  private String unidad;

  // Solo presente si el bien es BienConEstado
  private Boolean esNuevo;

  // Solo presente si el bien es BienPerecible
  private LocalDate fechaVencimiento;
}