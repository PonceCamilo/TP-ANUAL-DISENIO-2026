package ar.utn.donatrack.donaciones.exceptions.personasExceptions;

import ar.utn.donatrack.donaciones.models.donante.EstadoDonante;

public class CambioEstadoPersonaIlegalException extends RuntimeException {

  public CambioEstadoPersonaIlegalException(EstadoDonante actual, EstadoDonante nuevo) {
    super(String.format("Transición de estado no permitida: No es posible cambiar de %s a %s.",
        actual.name(), nuevo.name()));
  }
}
