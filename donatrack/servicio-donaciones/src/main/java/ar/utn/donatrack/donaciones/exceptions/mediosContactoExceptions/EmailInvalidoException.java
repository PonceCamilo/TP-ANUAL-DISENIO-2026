package ar.utn.donatrack.donaciones.exceptions.mediosContactoExceptions;

public class EmailInvalidoException extends RuntimeException {
  public EmailInvalidoException(String email) {
    super("El mail '" + email + "' esta vacio o tiene un formato invalido.");
  }
}
