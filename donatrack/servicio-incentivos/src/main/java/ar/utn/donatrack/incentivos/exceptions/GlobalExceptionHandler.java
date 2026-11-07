package ar.utn.donatrack.incentivos.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private Map<String, Object> construirCuerpoError(HttpStatus status, String error, String mensaje) {
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("timestamp", LocalDateTime.now());
        cuerpo.put("status", status.value());
        cuerpo.put("error", error);
        cuerpo.put("message", mensaje);
        return cuerpo;
    }

    private ResponseEntity<Map<String, Object>> construirRespuesta(HttpStatus status, String error, Exception ex) {
        return construirRespuesta(status, error, ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> construirRespuesta(HttpStatus status, String error, String mensaje) {
        return new ResponseEntity<>(construirCuerpoError(status, error, mensaje), status);
    }

    @ExceptionHandler(CategoriasDonadasInvalidasException.class)
    public ResponseEntity<Map<String, Object>> manejarCategoriasInvalidas(CategoriasDonadasInvalidasException ex) {
        return construirRespuesta(HttpStatus.BAD_REQUEST, "Bad Request", ex);
    }

    @ExceptionHandler(MisionNoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> manejarMisionNoEncontrada(MisionNoEncontradaException ex) {
        return construirRespuesta(HttpStatus.NOT_FOUND, "Not Found", ex);
    }

    @ExceptionHandler(DonanteNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> manejarDonanteNoEncontrado(DonanteNoEncontradoException ex) {
        return construirRespuesta(HttpStatus.NOT_FOUND, "Not Found", ex);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> manejarJsonInvalido(HttpMessageNotReadableException ex) {
        return construirRespuesta(HttpStatus.BAD_REQUEST, "Bad Request", ex);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> manejarValidacion(MethodArgumentNotValidException ex) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse(ex.getMessage());
        return construirRespuesta(HttpStatus.BAD_REQUEST, "Bad Request", detalle);
    }
}
