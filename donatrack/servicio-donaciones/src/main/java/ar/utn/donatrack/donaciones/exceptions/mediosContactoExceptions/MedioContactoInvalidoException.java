package ar.utn.donatrack.donaciones.exceptions.mediosContactoExceptions;

public class MedioContactoInvalidoException extends RuntimeException {
  public MedioContactoInvalidoException(String valor) {
    super("El medio de contacto '" + valor + "' es inválido: el valor no puede estar vacío");
  }
}
