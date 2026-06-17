package ar.utn.donatrack.donaciones.models.donacion;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DonacionPerecible extends Donacion{
  private LocalDate fechaVencimiento;
}
