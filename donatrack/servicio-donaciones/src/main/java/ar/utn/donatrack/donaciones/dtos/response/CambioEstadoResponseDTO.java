package ar.utn.donatrack.donaciones.dtos.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CambioEstadoResponseDTO {
    private final String estadoPrevio;
    private final String estado;
    private final String nombreTransicion;
    private final String justificacion;
    private final LocalDateTime fechaHora;
}
