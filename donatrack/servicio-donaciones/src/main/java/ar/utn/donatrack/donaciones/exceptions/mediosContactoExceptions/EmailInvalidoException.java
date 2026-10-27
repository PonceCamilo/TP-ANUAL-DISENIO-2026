package ar.utn.donatrack.donaciones.exceptions.mediosContactoExceptions;

import ar.utn.donatrack.donaciones.exceptions.SolicitudInvalidaException;

public class EmailInvalidoException extends SolicitudInvalidaException {
  public EmailInvalidoException(String email) {
    super("El mail '" + email + "' esta vacio o tiene un formato invalido.");
  }
}
