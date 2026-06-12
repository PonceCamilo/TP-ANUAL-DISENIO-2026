package ar.utn.donatrack.donaciones.exceptions.mediosContactoExceptions;

public class MedioContactoInvalidoException extends RuntimeException {
  public MedioContactoInvalidoException() {
    super("El medio de contacto es invalido");
  }
}
