package ar.utn.donatrack.donaciones.models.entidad.necesidad;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class NecesidadConPeriodo {
    private int id;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private int cantidadRecibida;
    private int cantidadObjetivo; // Copiamos el objetivo por si el día de mañana la necesidad cambia de cantidad

    public NecesidadConPeriodo(LocalDate fechaInicio, LocalDate fechaFin, int cantidadObjetivo) {
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.cantidadObjetivo = cantidadObjetivo;
        this.cantidadRecibida = 0;
    }

    public void recibirDonacion(int cantidad) {
        this.cantidadRecibida += cantidad;
    }

    public boolean estaSatisfecho() {
        return this.cantidadRecibida >= this.cantidadObjetivo;
    }

    public boolean contieneFecha(LocalDate fecha) {
        return !fecha.isBefore(fechaInicio) && !fecha.isAfter(fechaFin);
    }
}
