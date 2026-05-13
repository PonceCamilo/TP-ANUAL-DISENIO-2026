package ar.utn.donatrack.donaciones.model.donacion.bien;

import ar.utn.donatrack.donaciones.model.categoria.Subcategoria;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Bien {
    protected int idBien;
    protected Subcategoria subcategoria;
    protected String descripcion;
    protected String foto; // URL o path a la foto
    protected int cantidad;
    protected String unidad; // ej: "kg", "litros", "unidades"

    public Bien(Subcategoria subcategoria, String descripcion, String foto, int cantidad, String unidad) {
        this.subcategoria = subcategoria;
        this.descripcion = descripcion;
        this.foto = foto;
        this.cantidad = cantidad;
        this.unidad = unidad;
    }
}
