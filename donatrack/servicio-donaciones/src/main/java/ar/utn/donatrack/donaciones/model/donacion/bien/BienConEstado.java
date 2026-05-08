package ar.utn.donatrack.donaciones.model.donacion.bien;

import ar.utn.donatrack.donaciones.model.categoria.Subcategoria;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BienConEstado extends Bien {
    private boolean esNuevo;

    public BienConEstado(Subcategoria subcategoria, boolean esNuevo) {
        super(subcategoria);
        this.esNuevo = esNuevo;
    }
}
