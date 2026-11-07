package ar.utn.donatrack.donaciones.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cambio de estado manual de una donación. El nombre de la transición ya no se
 * recibe: lo deriva la donación a partir del estado de origen y el de destino
 * (si un cliente todavía lo manda, se ignora).
 */
@Getter
@Setter
@NoArgsConstructor
public class CambioEstadoRequestDTO {
    @NotBlank
    private String estado;
    private String justificacion;
}
