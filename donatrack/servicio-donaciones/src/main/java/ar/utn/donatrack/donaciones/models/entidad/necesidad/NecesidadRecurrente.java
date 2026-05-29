package ar.utn.donatrack.donaciones.models.entidad.necesidad;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Necesidad;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.NecesidadConPeriodo;
import java.util.List;
import java.util.ArrayList;

@Getter
@Setter
public class NecesidadRecurrente extends Necesidad {
    private Periodicidad periodo;
    private List<NecesidadConPeriodo> historialPeriodos = new ArrayList<>();

    @Override
    public void recibirDonacion(int cantidad) {
        NecesidadConPeriodo periodoActual = obtenerOGenerarPeriodoActual(LocalDate.now());
        periodoActual.recibirDonacion(cantidad);
    }

    @Override
    public boolean estaSatisfecha() {
        NecesidadConPeriodo periodoActual = obtenerOGenerarPeriodoActual(LocalDate.now());
        return periodoActual.estaSatisfecho();
    }

    // busca el periodo actual en el historial y si no encuentra lo crea
    public NecesidadConPeriodo obtenerOGenerarPeriodoActual(LocalDate fecha) {
        return historialPeriodos.stream()
                .filter(p -> p.contieneFecha(fecha))
                .findFirst()
                .orElseGet(() -> iniciarNuevoPeriodo(fecha));
    }

    private NecesidadConPeriodo iniciarNuevoPeriodo(LocalDate fechaInicio) {
        LocalDate fechaFin = calcularFechaFin(fechaInicio);  // la fecha en que termina depende del tipo de periodo
        NecesidadConPeriodo nuevoPeriodo = new NecesidadConPeriodo(fechaInicio, fechaFin, this.cantidadObjetivo);
        this.historialPeriodos.add(nuevoPeriodo);
        return nuevoPeriodo;
    }

    private LocalDate calcularFechaFin(LocalDate inicio) {
        return switch (this.periodo) {
            case SEMANAL -> inicio.plusWeeks(1).minusDays(1);
            case MENSUAL -> inicio.plusMonths(1).minusDays(1);
            case CUATRIMESTRAL -> inicio.plusMonths(4).minusDays(1);
            case ANUAL -> inicio.plusYears(1).minusDays(1);
        };
    }
    
    public boolean periodoVencido(LocalDate fechaActual) {
        if (historialPeriodos.isEmpty()) return true;
        NecesidadConPeriodo ultimo = historialPeriodos.get(historialPeriodos.size() - 1);
        return fechaActual.isAfter(ultimo.getFechaFin());
    }


    //TODO ver si es necesario mantener un historial de periodos anteriores, o si con reiniciar el periodo y mantener la cantidad recibida es suficiente
    //public void reiniciarPeriodo() {
    //    this.cantidadRecibida = 0;
    //    this.fechaInicioPeriodo = LocalDate.now();
    //}
    //
    //public boolean requiereReinicio(LocalDate fechaActual) {
    //    if (fechaInicioPeriodo == null) return false;
    //
    //    return switch (this.periodo) {
    //        case SEMANAL -> !fechaInicioPeriodo.plusWeeks(1).isAfter(fechaActual);
    //        case MENSUAL -> fechaInicioPeriodo.plusMonths(1).isBefore(fechaActual);
    //        case CUATRIMESTRAL -> fechaInicioPeriodo.plusMonths(4).isBefore(fechaActual);
    //        case ANUAL -> fechaInicioPeriodo.plusYears(1).isBefore(fechaActual);
    //    };
    //}
    //
}