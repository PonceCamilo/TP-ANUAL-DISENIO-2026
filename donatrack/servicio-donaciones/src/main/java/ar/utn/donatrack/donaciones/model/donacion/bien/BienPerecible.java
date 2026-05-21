package ar.utn.donatrack.donaciones.model.donacion.bien;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
public class BienPerecible extends Bien {
    private LocalDate fechaVencimiento;
}
