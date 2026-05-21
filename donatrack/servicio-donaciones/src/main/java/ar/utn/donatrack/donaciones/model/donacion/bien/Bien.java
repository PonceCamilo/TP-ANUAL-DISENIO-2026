package ar.utn.donatrack.donaciones.model.donacion.bien;

import ar.utn.donatrack.donaciones.model.categoria.Subcategoria;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
public abstract class Bien {
    protected int idBien;
    protected Subcategoria subcategoria;
    protected String descripcion;
    protected String foto; // URL o path a la foto
    protected int cantidad;
    protected String unidad; // ej: "kg", "litros", "unidades"
}
