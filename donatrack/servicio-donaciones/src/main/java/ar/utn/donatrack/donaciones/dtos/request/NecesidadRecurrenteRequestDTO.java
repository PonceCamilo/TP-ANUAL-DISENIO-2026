package ar.utn.donatrack.donaciones.dtos.request;

import ar.utn.donatrack.donaciones.models.entidad.necesidad.periodicidades.Periodicidad;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class NecesidadRecurrenteRequestDTO extends NecesidadRequestDTO {

    @NotNull(message = "Debe indicar la periodicidad de la necesidad recurrente")
    private Periodicidad periodo;
    // Spring boot transforma automáticamente el texto del JSON (ej: "MENSUAL") al Enum Periodicidad
}