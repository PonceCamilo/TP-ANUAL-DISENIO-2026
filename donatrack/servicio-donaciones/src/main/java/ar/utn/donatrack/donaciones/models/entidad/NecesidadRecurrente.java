package ar.utn.donatrack.donaciones.models.entidad;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class NecesidadRecurrente extends Necesidad {
    private Periodicidad periodo;
    private LocalDate fechaInicioPeriodo;

    //TODO ver si es necesario mantener un historial de periodos anteriores, o si con reiniciar el periodo y mantener la cantidad recibida es suficiente
    public void reiniciarPeriodo() {
        this.cantidadRecibida = 0;
        this.fechaInicioPeriodo = LocalDate.now();
    }

    public boolean requiereReinicio(LocalDate fechaActual) {
        if (fechaInicioPeriodo == null) return false;

        return switch (this.periodo) {
            case SEMANAL -> !fechaInicioPeriodo.plusWeeks(1).isAfter(fechaActual);
            case MENSUAL -> fechaInicioPeriodo.plusMonths(1).isBefore(fechaActual);
            case CUATRIMESTRAL -> fechaInicioPeriodo.plusMonths(4).isBefore(fechaActual);
            case ANUAL -> fechaInicioPeriodo.plusYears(1).isBefore(fechaActual);
        };
    }
    
}