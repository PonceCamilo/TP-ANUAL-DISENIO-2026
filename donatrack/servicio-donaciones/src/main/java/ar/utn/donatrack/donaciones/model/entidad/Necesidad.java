package ar.utn.donatrack.donaciones.model.entidad;

import ar.utn.donatrack.donaciones.model.categoria.Subcategoria;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Necesidad {
    protected int id;
    protected Subcategoria subcategoria;
    protected String descripcion;
    protected int cantidadObjetivo;
    protected int cantidadRecibida;

    public void recibirDonacion(int cantidad){
        this.cantidadRecibida += cantidad;
    }

    public boolean estaSatisfecha() {
        return this.cantidadRecibida >= this.cantidadObjetivo;
    }
}