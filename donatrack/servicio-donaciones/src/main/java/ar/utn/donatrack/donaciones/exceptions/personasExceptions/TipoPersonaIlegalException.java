package ar.utn.donatrack.donaciones.exceptions.personasExceptions;

import java.util.UUID;

public class TipoPersonaIlegalException extends RuntimeException {
  public TipoPersonaIlegalException(UUID id) {
    super("La persona donante con id " + id + " no es jurídica");
  }
}
