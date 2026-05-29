package ar.utn.donatrack.donaciones.models.entidad.necesidad;

import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Necesidad {
    protected int id;
    protected Subcategoria subcategoria;
    protected String descripcion;
    protected int cantidadObjetivo;

    public abstract void recibirDonacion(int cantidad);
    public abstract boolean estaSatisfecha();
}