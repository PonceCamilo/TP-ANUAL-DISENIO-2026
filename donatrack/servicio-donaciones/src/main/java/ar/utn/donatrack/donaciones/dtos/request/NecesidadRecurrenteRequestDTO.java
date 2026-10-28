package ar.utn.donatrack.donaciones.dtos.request;

import ar.utn.donatrack.donaciones.models.entidad.necesidad.Necesidad;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.NecesidadRecurrente;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.TipoNecesidad;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.periodicidades.Periodicidad;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class NecesidadRecurrenteRequestDTO extends NecesidadRequestDTO {

    /** Spring convierte el texto del JSON (ej: "MENSUAL") en el enum. */
    @NotNull(message = "Debe indicar la periodicidad de la necesidad recurrente")
    private Periodicidad periodo;

    @Override
    public TipoNecesidad tipo() {
        return TipoNecesidad.RECURRENTE;
    }

    /** El primer período arranca el día del alta. */
    @Override
    public Necesidad crear(LocalDate hoy) {
        NecesidadRecurrente necesidad = completar(new NecesidadRecurrente(), hoy);
        necesidad.setPeriodo(periodo);
        necesidad.setFechaInicioPeriodo(hoy);
        return necesidad;
    }

    /** Cambia la periodicidad sin reiniciar el período en curso. */
    @Override
    public void aplicarEn(Necesidad necesidad) {
        super.aplicarEn(necesidad);
        ((NecesidadRecurrente) necesidad).setPeriodo(periodo);
    }
}
