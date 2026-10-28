package ar.utn.donatrack.donaciones.exceptions.personasExceptions;

import ar.utn.donatrack.donaciones.exceptions.SolicitudInvalidaException;

import java.util.UUID;

public class TipoPersonaIlegalException extends SolicitudInvalidaException {
    public TipoPersonaIlegalException(UUID id) {
        super("La persona donante con id " + id + " no es jurídica");
    }
}
