package ar.utn.donatrack.donaciones.exceptions;

import ar.utn.donatrack.donaciones.exceptions.cambioEstadosExceptions.CambioEstadoDonacionIlegalException;
import ar.utn.donatrack.donaciones.exceptions.cambioEstadosExceptions.FaltaJustificacionDonacionException;
import ar.utn.donatrack.donaciones.exceptions.cambioEstadosExceptions.FaltaJustificacionException;
import ar.utn.donatrack.donaciones.exceptions.donacionesExceptions.DonacionNoEncontradaException;
import ar.utn.donatrack.donaciones.exceptions.entidadesExceptions.CampaniaNoEncontradaException;
import ar.utn.donatrack.donaciones.exceptions.entidadesExceptions.EntidadBeneficiariaNoEncontradaException;
import ar.utn.donatrack.donaciones.exceptions.entidadesExceptions.FechasCampaniaInvalidasException;
import ar.utn.donatrack.donaciones.exceptions.mediosContactoExceptions.EmailInvalidoException;
import ar.utn.donatrack.donaciones.exceptions.mediosContactoExceptions.EmailYaRegistradoException;
import ar.utn.donatrack.donaciones.exceptions.mediosContactoExceptions.MedioContactoInvalidoException;
import ar.utn.donatrack.donaciones.exceptions.personasExceptions.CambioEstadoPersonaIlegalException;
import ar.utn.donatrack.donaciones.exceptions.personasExceptions.EstadoNoExistenteException;
import ar.utn.donatrack.donaciones.exceptions.personasExceptions.PersonaConMismoEstadoException;
import ar.utn.donatrack.donaciones.exceptions.personasExceptions.PersonaDonanteNoEncontradaException;
import ar.utn.donatrack.donaciones.exceptions.personasExceptions.TipoPersonaIlegalException;
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

  @ExceptionHandler(PersonaDonanteNoEncontradaException.class)
  public ResponseEntity<Map<String, Object>> manejarDonanteNoEncontrado(PersonaDonanteNoEncontradaException ex) {
    return new ResponseEntity<>(construirCuerpoError(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage()), HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(DonacionNoEncontradaException.class)
  public ResponseEntity<Map<String, Object>> manejarDonacionNoEncontrada(DonacionNoEncontradaException ex) {
    return new ResponseEntity<>(construirCuerpoError(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage()), HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(EmailInvalidoException.class)
  public ResponseEntity<Map<String, Object>> manejarEmailInvalido(EmailInvalidoException ex) {
    return new ResponseEntity<>(construirCuerpoError(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage()), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(FaltaJustificacionException.class)
  public ResponseEntity<Map<String, Object>> manejarFaltaJustificacion(FaltaJustificacionException ex) {
    return new ResponseEntity<>(construirCuerpoError(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage()), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(FaltaJustificacionDonacionException.class)
  public ResponseEntity<Map<String, Object>> manejarFaltaJustificacionDonacion(FaltaJustificacionDonacionException ex) {
    return new ResponseEntity<>(construirCuerpoError(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage()), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(TipoPersonaIlegalException.class)
  public ResponseEntity<Map<String, Object>> manejarTipoPersonaIlegal(TipoPersonaIlegalException ex) {
    return new ResponseEntity<>(construirCuerpoError(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage()), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(MedioContactoInvalidoException.class)
  public ResponseEntity<Map<String, Object>> manejarMedioContactoInvalido(MedioContactoInvalidoException ex) {
    return new ResponseEntity<>(construirCuerpoError(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage()), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(EmailYaRegistradoException.class)
  public ResponseEntity<Map<String, Object>> manejarEmailYaRegistrado(EmailYaRegistradoException ex) {
    return new ResponseEntity<>(construirCuerpoError(HttpStatus.CONFLICT, "Conflict", ex.getMessage()), HttpStatus.CONFLICT);
  }

  @ExceptionHandler(PersonaConMismoEstadoException.class)
  public ResponseEntity<Map<String, Object>> manejarPersonaConMismoEstado(PersonaConMismoEstadoException ex) {
    return new ResponseEntity<>(construirCuerpoError(HttpStatus.CONFLICT, "Conflict", ex.getMessage()), HttpStatus.CONFLICT);
  }

  @ExceptionHandler(CambioEstadoDonacionIlegalException.class)
  public ResponseEntity<Map<String, Object>> manejarCambioEstadoIlegal(CambioEstadoDonacionIlegalException ex) {
    return new ResponseEntity<>(construirCuerpoError(HttpStatus.UNPROCESSABLE_ENTITY, "Unprocessable Entity", ex.getMessage()), HttpStatus.UNPROCESSABLE_ENTITY);
  }

  @ExceptionHandler(CambioEstadoPersonaIlegalException.class)
  public ResponseEntity<Map<String, Object>> manejarCambioEstadoPersonaIlegal(CambioEstadoPersonaIlegalException ex) {
    return new ResponseEntity<>(construirCuerpoError(HttpStatus.UNPROCESSABLE_ENTITY, "Unprocessable Entity", ex.getMessage()), HttpStatus.UNPROCESSABLE_ENTITY);
  }

  @ExceptionHandler(EntidadBeneficiariaNoEncontradaException.class)
  public ResponseEntity<Map<String, Object>> manejarEntidadBeneficiariaNoEncontrada(ar.utn.donatrack.donaciones.exceptions.entidadesExceptions.EntidadBeneficiariaNoEncontradaException ex) {
    return new ResponseEntity<>(construirCuerpoError(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage()), HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(CampaniaNoEncontradaException.class)
  public ResponseEntity<Map<String, Object>> manejarCampaniaNoEncontrada(ar.utn.donatrack.donaciones.exceptions.entidadesExceptions.CampaniaNoEncontradaException ex) {
    return new ResponseEntity<>(construirCuerpoError(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage()), HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(EstadoNoExistenteException.class)
  public ResponseEntity<Map<String, Object>> manejarEstadoNoExistente(EstadoNoExistenteException ex) {
    return new ResponseEntity<>(construirCuerpoError(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage()), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(FechasCampaniaInvalidasException.class)
  public ResponseEntity<Map<String, Object>> manejarFechasCampaniaInvalidas(FechasCampaniaInvalidasException ex) {
    return new ResponseEntity<>(construirCuerpoError(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage()), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<Map<String, Object>> manejarEstadoIlegal(IllegalStateException ex) {
    return new ResponseEntity<>(construirCuerpoError(HttpStatus.CONFLICT, "Conflict", ex.getMessage()), HttpStatus.CONFLICT);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<Map<String, Object>> manejarJsonInvalido(HttpMessageNotReadableException ex) {
    return new ResponseEntity<>(
        construirCuerpoError(HttpStatus.BAD_REQUEST, "Bad Request",
            "El cuerpo de la petición es inválido o está mal formado (revise tipos y valores enumerados)."),
        HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> manejarValidacion(MethodArgumentNotValidException ex) {
    String detalle = ex.getBindingResult().getFieldErrors().stream()
        .map(e -> e.getField() + ": " + e.getDefaultMessage())
        .reduce((a, b) -> a + "; " + b)
        .orElse("Datos inválidos");
    return new ResponseEntity<>(construirCuerpoError(HttpStatus.BAD_REQUEST, "Bad Request", detalle), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> manejarErrorGeneral(Exception ex) {
    return new ResponseEntity<>(
        construirCuerpoError(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
            "Error inesperado. Contacte al administrador."),
        HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
