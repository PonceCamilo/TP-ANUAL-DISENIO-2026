package ar.utn.donatrack.logistica.exceptions;

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

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> manejarIlegal(IllegalArgumentException ex) {
        return construir(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    @ExceptionHandler(TransicionEntregaIlegalException.class)
    public ResponseEntity<Map<String, Object>> manejarTransicionIlegal(TransicionEntregaIlegalException ex) {
        return construir(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    @ExceptionHandler(LoteCallbackInvalidoException.class)
    public ResponseEntity<Map<String, Object>> manejarCallbackInvalido(LoteCallbackInvalidoException ex) {
        return construir(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    @ExceptionHandler({
            CamionNoEncontradoException.class,
            LoteNoEncontradoException.class,
            RutaNoEncontradaException.class,
            EntregaNoEncontradaException.class
    })
    public ResponseEntity<Map<String, Object>> manejarNoEncontrado(RuntimeException ex) {
        return construir(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
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
