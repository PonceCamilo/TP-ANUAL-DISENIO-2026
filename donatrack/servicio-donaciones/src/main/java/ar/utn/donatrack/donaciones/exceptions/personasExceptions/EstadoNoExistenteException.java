package ar.utn.donatrack.donaciones.exceptions.personasExceptions;

import ar.utn.donatrack.donaciones.models.donante.EstadoDonante;

public class EstadoNoExistenteException extends RuntimeException {
  public EstadoNoExistenteException(EstadoDonante estado) {
    super("El estado " + estado + " no existe");
  }
}
