package ar.utn.donatrack.donaciones.exceptions.personasExceptions;

import ar.utn.donatrack.donaciones.models.donante.EstadoDonante;

public class PersonaConMismoEstadoException extends RuntimeException {
  public PersonaConMismoEstadoException(EstadoDonante estado) {
    super("La persona donante ya se encuentra con el estado: " + estado);
  }
}
