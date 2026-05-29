package ar.utn.donatrack.donaciones.models.entidad.necesidad;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.periodicidades.Periodicidad;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class NecesidadRecurrente extends Necesidad {
    private Periodicidad periodo;

    public boolean periodoVencido(){
        LocalDate fechaActual = LocalDate.now();
        LocalDate fechaVencimiento = this.fechaRegistro.plusDays(periodo.getDias());

        return fechaActual.isAfter(fechaVencimiento);
    }

    @Override
    public boolean estaSatisfecha() {
        return (this.cantidadRecibida >= this.cantidadObjetivo && !periodoVencido());
    }
}