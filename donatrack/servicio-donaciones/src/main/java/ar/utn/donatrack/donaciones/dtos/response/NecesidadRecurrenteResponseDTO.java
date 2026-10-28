package ar.utn.donatrack.donaciones.dtos.response;

import ar.utn.donatrack.donaciones.models.entidad.necesidad.periodicidades.Periodicidad;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Getter
@SuperBuilder
public class NecesidadRecurrenteResponseDTO extends NecesidadResponseDTO {

    private final Periodicidad periodo;
    private final LocalDate fechaInicioPeriodo;
}
