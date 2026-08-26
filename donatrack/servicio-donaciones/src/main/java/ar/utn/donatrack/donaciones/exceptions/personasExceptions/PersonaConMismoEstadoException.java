package ar.utn.donatrack.donaciones.exceptions.personasExceptions;

public class PersonaConMismoEstadoException extends RuntimeException {
  public PersonaConMismoEstadoException(String estado) {
    super("La persona donante ya se encuentra con el estado: " + estado);
  }
}
