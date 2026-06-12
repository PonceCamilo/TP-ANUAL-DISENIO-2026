package ar.utn.donatrack.donaciones.exceptions.cambioEstadosExceptions;

public class FaltaJustificacionException extends RuntimeException {
  public FaltaJustificacionException() {
    super("Es necesaria una justificacion para cambiar el estado de la donacion.");
  }
}
