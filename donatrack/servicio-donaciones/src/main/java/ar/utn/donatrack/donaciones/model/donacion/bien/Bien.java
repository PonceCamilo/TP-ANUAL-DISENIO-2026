package ar.utn.donatrack.donaciones.model.donacion.bien;

import ar.utn.donatrack.donaciones.model.categoria.Subcategoria;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Bien {
    // TODO: descripcion, foto (opcional), subcategoria, cantidad, unidad
    protected Subcategoria subcategoria;
    protected String descripcion;
    protected String foto; // URL o path a la foto
    protected int cantidad;
    protected String unidad; // ej: "kg", "litros", "unidades"

    public Bien(Subcategoria subcategoria) {
        this.subcategoria = subcategoria;
    }
}
