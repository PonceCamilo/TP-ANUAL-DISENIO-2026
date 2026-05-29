package ar.utn.donatrack.donaciones.models.donacion.bien;

import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
public abstract class Bien {
    protected Subcategoria subcategoria;
    protected String descripcion;
    protected String foto; // URL o path a la foto
    protected int cantidad;
    protected String unidad; // ej: "kg", "litros", "unidades"
}
