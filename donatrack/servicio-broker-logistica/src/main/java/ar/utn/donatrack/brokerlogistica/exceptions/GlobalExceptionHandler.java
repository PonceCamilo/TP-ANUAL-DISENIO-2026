package ar.utn.donatrack.brokerlogistica.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> manejarValidacion(MethodArgumentNotValidException ex) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Datos inválidos");
        return construir(HttpStatus.BAD_REQUEST, "Bad Request", detalle);
    }

    @ExceptionHandler({ProveedorDesconocidoException.class, EventoInvalidoException.class})
    public ResponseEntity<Map<String, Object>> manejarPedidoInvalido(RuntimeException ex) {
        return construir(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    @ExceptionHandler(EnvioNoRegistradoException.class)
    public ResponseEntity<Map<String, Object>> manejarNoEncontrado(EnvioNoRegistradoException ex) {
        return construir(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    @ExceptionHandler({SinProveedorDisponibleException.class, ProveedorNoDisponibleException.class})
    public ResponseEntity<Map<String, Object>> manejarSinProveedor(RuntimeException ex) {
        return construir(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", ex.getMessage());
    }

    @ExceptionHandler(DonacionesNoDisponibleException.class)
    public ResponseEntity<Map<String, Object>> manejarDonacionesCaido(DonacionesNoDisponibleException ex) {
        return construir(HttpStatus.BAD_GATEWAY, "Bad Gateway", ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> construir(HttpStatus status, String error, String mensaje) {
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("timestamp", LocalDateTime.now());
        cuerpo.put("status", status.value());
        cuerpo.put("error", error);
        cuerpo.put("message", mensaje);
        return new ResponseEntity<>(cuerpo, status);
    }
}
