package ar.utn.donatrack.donaciones.dtos.response;

import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

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

    /**
     * El error con la hora actual de Argentina y el texto de `error` sacado del
     * propio código HTTP. Lo usan GlobalExceptionHandler y, para los 401 y 403
     * que corta la seguridad antes de llegar a un controller, SeguridadConfig
     * (Entrega 6): así toda respuesta de error tiene la misma forma.
     */
    public static ErrorResponseDTO de(HttpStatus status, String mensaje) {
        return ErrorResponseDTO.builder()
                .timestamp(FechaHoraArgentina.ahora())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(mensaje)
                .build();
    }
}
