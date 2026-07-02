package ar.utn.donatrack.incentivos.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

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
        return new ResponseEntity<>(construirCuerpoError(status, error, ex.getMessage()), status);
    }

    @ExceptionHandler(CategoriasDonadasInvalidasException.class)
    public ResponseEntity<Map<String, Object>> manejarCategoriasInvalidas(CategoriasDonadasInvalidasException ex) {
        return construirRespuesta(HttpStatus.BAD_REQUEST, "Bad Request", ex);
    }

    @ExceptionHandler(MisionNoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> manejarMisionNoEncontrada(MisionNoEncontradaException ex) {
        return construirRespuesta(HttpStatus.NOT_FOUND, "Not Found", ex);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> manejarIlegal(IllegalArgumentException ex) {
        return construirRespuesta(HttpStatus.BAD_REQUEST, "Bad Request", ex);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, Object>> manejarNoEncontrado(NoSuchElementException ex) {
        return construirRespuesta(HttpStatus.NOT_FOUND, "Not Found", ex);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> manejarJsonInvalido(HttpMessageNotReadableException ex) {
        return construirRespuesta(HttpStatus.BAD_REQUEST, "Bad Request", ex);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> manejarValidacion(MethodArgumentNotValidException ex) {
        return construirRespuesta(HttpStatus.BAD_REQUEST, "Bad Request", ex);
    }
}
