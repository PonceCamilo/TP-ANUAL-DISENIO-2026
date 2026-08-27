package ar.utn.donatrack.donaciones.exceptions.personasExceptions;

public class CambioEstadoPersonaIlegalException extends RuntimeException {

  public CambioEstadoPersonaIlegalException(String actual, String nuevo) {
    super(String.format("Transición de estado no permitida: No es posible cambiar de %s a %s.",
        actual, nuevo));
  }
}
