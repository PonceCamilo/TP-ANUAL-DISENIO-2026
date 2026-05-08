package ar.utn.donatrack.donaciones.model.donacion.bien;

import java.time.LocalDate;

import ar.utn.donatrack.donaciones.model.categoria.Subcategoria;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BienPerecible extends Bien {
    private LocalDate fechaVencimiento;

    public BienPerecible(Subcategoria subcategoria, LocalDate fechaVencimiento) {
        super(subcategoria);
        this.fechaVencimiento = fechaVencimiento;
    }
}
