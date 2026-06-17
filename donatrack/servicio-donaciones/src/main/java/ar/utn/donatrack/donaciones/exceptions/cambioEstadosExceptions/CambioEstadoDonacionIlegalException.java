package ar.utn.donatrack.donaciones.exceptions.cambioEstadosExceptions;

import ar.utn.donatrack.donaciones.models.donacion.EstadoDonacion;

public class CambioEstadoDonacionIlegalException extends RuntimeException {
  public CambioEstadoDonacionIlegalException(EstadoDonacion estadoActual, EstadoDonacion estadoNuevo) {
    super("Transición inválida: " + estadoActual + " → " + estadoNuevo);
  }
}
