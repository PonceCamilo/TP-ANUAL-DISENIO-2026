package ar.utn.donatrack.donaciones.exceptions.cambioEstadosExceptions;

import ar.utn.donatrack.donaciones.models.donacion.EstadoDonacion;

public class CambioEstadoIlegalException extends RuntimeException {
  public CambioEstadoIlegalException(EstadoDonacion estadoActual, EstadoDonacion estadoNuevo) {
    super("Transición inválida: " + estadoActual + " → " + estadoNuevo);
  }
}
