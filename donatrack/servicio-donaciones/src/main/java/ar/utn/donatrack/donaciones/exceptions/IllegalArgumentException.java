package ar.utn.donatrack.donaciones.exceptions;

public class IllegalArgumentException extends RuntimeException {

  public IllegalArgumentException() {
    super("Solo se pueden agregar representantes a personas jurídicas.");
  }
}
