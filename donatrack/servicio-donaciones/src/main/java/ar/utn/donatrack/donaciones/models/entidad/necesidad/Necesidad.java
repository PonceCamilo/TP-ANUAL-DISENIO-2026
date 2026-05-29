package ar.utn.donatrack.donaciones.models.entidad.necesidad;

import ar.utn.donatrack.donaciones.models.donacion.bien.Bien;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Necesidad {
    protected Bien bien;
    protected String descripcion;
    protected int cantidadObjetivo;
    protected int cantidadRecibida;

    public abstract void recibirDonacion(int cantidad);
    public abstract boolean estaSatisfecha();
}