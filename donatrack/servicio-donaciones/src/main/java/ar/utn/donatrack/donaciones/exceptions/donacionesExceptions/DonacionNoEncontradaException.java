package ar.utn.donatrack.donaciones.exceptions.donacionesExceptions;

import ar.utn.donatrack.donaciones.exceptions.NoEncontradoException;

import java.util.UUID;

public class DonacionNoEncontradaException extends NoEncontradoException {
  public DonacionNoEncontradaException(UUID id) {
    super("Donación no encontrada con id: " + id);
  }
}
