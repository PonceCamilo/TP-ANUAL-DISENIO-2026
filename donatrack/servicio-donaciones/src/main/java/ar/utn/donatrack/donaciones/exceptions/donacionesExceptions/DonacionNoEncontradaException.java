package ar.utn.donatrack.donaciones.exceptions.donacionesExceptions;

import java.util.UUID;

public class DonacionNoEncontradaException extends RuntimeException {
  public DonacionNoEncontradaException(UUID id) {
    super("Donación no encontrada con id: " + id);
  }
}
