package ar.utn.donatrack.donaciones.exceptions.mediosContactoExceptions;

import ar.utn.donatrack.donaciones.exceptions.SolicitudInvalidaException;

public class MedioContactoInvalidoException extends SolicitudInvalidaException {
  public MedioContactoInvalidoException(String valor) {
    super("El medio de contacto '" + valor + "' es inválido: el valor no puede estar vacío");
  }
}
