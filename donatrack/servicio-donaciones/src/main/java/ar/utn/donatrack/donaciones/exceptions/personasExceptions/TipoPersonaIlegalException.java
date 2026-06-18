package ar.utn.donatrack.donaciones.exceptions.personasExceptions;

public class TipoPersonaIlegalException extends RuntimeException {
  public TipoPersonaIlegalException() {
    super("La persona no es juridica");
  }
}
