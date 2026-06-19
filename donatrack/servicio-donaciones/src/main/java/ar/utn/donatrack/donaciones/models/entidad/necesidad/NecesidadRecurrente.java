package ar.utn.donatrack.donaciones.models.entidad.necesidad;

import ar.utn.donatrack.donaciones.models.entidad.necesidad.periodicidades.Periodicidad;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Necesidad vinculada al funcionamiento habitual de la organización.
 * Se satisface dentro de cada período (ej: 100 paquetes de fideos por semana).
 * Al vencer el período, la cantidad recibida se reinicia para el período siguiente.
 *
 * Campo clave: fechaInicioPeriodo — marca el inicio del período vigente.
 * Se usa en lugar de fechaRegistro para poder reiniciarla sin perder el historial.
 */

@Getter
@Setter
public class NecesidadRecurrente extends Necesidad {

    private Periodicidad periodo;

    // Inicio del período actual. Se actualiza cada vez que comienza un período nuevo.
    private LocalDate fechaInicioPeriodo;

    /**
     * Determina si el período vigente ya venció respecto a una fecha de referencia.
     * Recibe la fecha como parámetro para facilitar los tests (sin depender de LocalDate.now()).
     */
    public boolean periodoVencido(LocalDate referencia) {
        if (fechaInicioPeriodo == null) return false;
        LocalDate fechaVencimiento = fechaInicioPeriodo.plusDays(periodo.getDias());
        return referencia.isAfter(fechaVencimiento);
    }

    /**
     * Si el período está vencido respecto a `hoy`, reinicia el contador de cantidad recibida
     * y actualiza la fecha de inicio del período.
     * Si el período sigue vigente, no hace nada.
     */
    public void obtenerOGenerarPeriodoActual(LocalDate hoy) {
        if (periodoVencido(hoy)) {
            this.cantidadRecibida = 0;
            this.fechaInicioPeriodo = hoy;
        }
    }

    /**
     * Una necesidad recurrente está satisfecha cuando se alcanzó el objetivo
     * dentro del período vigente (es decir, el período no venció).
     */
    @Override
    public boolean estaSatisfecha() {
        return cantidadRecibida >= cantidadObjetivo && !periodoVencido(LocalDate.now());
    }
}
