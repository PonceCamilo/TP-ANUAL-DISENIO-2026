package ar.utn.donatrack.donaciones.exceptions.personasExceptions;

public class FaltaJustificacionException extends RuntimeException {
  public FaltaJustificacionException(String mensaje) {
    super(mensaje);
  }
}
