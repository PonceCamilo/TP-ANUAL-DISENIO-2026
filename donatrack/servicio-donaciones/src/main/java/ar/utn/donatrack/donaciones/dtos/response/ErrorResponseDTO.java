package ar.utn.donatrack.donaciones.dtos.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Cuerpo de toda respuesta de error. El orden de los campos es fijo: antes el
 * handler armaba un HashMap y las claves salían en un orden distinto en cada
 * respuesta.
 */
@Getter
@Builder
@JsonPropertyOrder({"timestamp", "status", "error", "message"})
public class ErrorResponseDTO {
    private final LocalDateTime timestamp;
    private final int status;
    private final String error;
    private final String message;
}
