package ar.utn.donatrack.donaciones.dtos.response;

import ar.utn.donatrack.donaciones.models.donacion.bien.TipoBien;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class BienResponseDTO {
  private UUID id;
  private TipoBien tipo;
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