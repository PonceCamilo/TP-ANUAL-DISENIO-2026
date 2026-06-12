package ar.utn.donatrack.donaciones.exceptions;

import ar.utn.donatrack.donaciones.exceptions.mediosContactoExceptions.EmailInvalidoException;
import ar.utn.donatrack.donaciones.exceptions.mediosContactoExceptions.EmailYaRegistradoException;
import ar.utn.donatrack.donaciones.exceptions.personasExceptions.PersonaDonanteNoEncontradaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

  @ExceptionHandler(PersonaDonanteNoEncontradaException.class)
  public ResponseEntity<Map<String, Object>> manejarDonanteNoEncontrado(PersonaDonanteNoEncontradaException ex) {
    Map<String, Object> error = construirCuerpoError(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(EmailInvalidoException.class)
  public ResponseEntity<Map<String, Object>> manejarEmailInvalido(EmailInvalidoException ex) {
    Map<String, Object> error = construirCuerpoError(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(EmailYaRegistradoException.class)
  public ResponseEntity<Map<String, Object>> manejarEmailYaRegistrado(EmailYaRegistradoException ex) {
    Map<String, Object> error = construirCuerpoError(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    return new ResponseEntity<>(error, HttpStatus.CONFLICT);
  }
}