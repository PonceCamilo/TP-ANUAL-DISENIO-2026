package ar.utn.donatrack.donaciones.exceptions.mediosContactoExceptions;

public class EmailInvalidoException extends RuntimeException {
  public EmailInvalidoException() {
    super("El mail no puede ser nulo o vacío.");
  }
}
