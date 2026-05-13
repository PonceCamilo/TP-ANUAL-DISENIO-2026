package ar.utn.donatrack.donaciones.model.donacion.bien;

import ar.utn.donatrack.donaciones.model.categoria.Subcategoria;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class BienConEstado extends Bien {
    private boolean esNuevo;

    public BienConEstado(Subcategoria subcategoria, String descripcion,
                         String foto, int cantidad, String unidad, boolean esNuevo) {
        super(subcategoria, descripcion, foto, cantidad, unidad);
        this.esNuevo = esNuevo;
    }
}
