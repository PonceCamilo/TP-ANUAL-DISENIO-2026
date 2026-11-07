package ar.utn.donatrack.logistica.dtos.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Posición que informa el chofer desde el celular. La hora no viaja en el
 * body: la pone el servidor al guardar, para que sea la última actualización.
 */
@Getter
@Setter
@NoArgsConstructor
public class UbicacionRequestDTO {
    @NotNull(message = "La latitud es obligatoria")
    @DecimalMin(value = "-90.0", message = "La latitud mínima es -90")
    @DecimalMax(value = "90.0", message = "La latitud máxima es 90")
    private Double latitud;

    @NotNull(message = "La longitud es obligatoria")
    @DecimalMin(value = "-180.0", message = "La longitud mínima es -180")
    @DecimalMax(value = "180.0", message = "La longitud máxima es 180")
    private Double longitud;
}
