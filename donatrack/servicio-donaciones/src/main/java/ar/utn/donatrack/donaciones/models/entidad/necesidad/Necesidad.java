package ar.utn.donatrack.donaciones.models.entidad.necesidad;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Raíz de la jerarquía de necesidades de una EntidadBeneficiaria.
 * Subclases: NecesidadExtraordinaria, NecesidadRecurrente.
 */

@Getter
@Setter
public abstract class Necesidad {

    protected String nombre;
    protected String descripcion;
    protected LocalDate fechaRegistro;
    protected int cantidadObjetivo;
    protected int cantidadRecibida;

    public void recibirDonacion(int cantidad) {
        this.cantidadRecibida += cantidad;
    }

    public abstract boolean estaSatisfecha();
}
