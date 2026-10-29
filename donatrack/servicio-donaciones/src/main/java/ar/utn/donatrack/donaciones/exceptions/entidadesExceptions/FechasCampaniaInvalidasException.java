package ar.utn.donatrack.donaciones.exceptions.entidadesExceptions;

import ar.utn.donatrack.donaciones.exceptions.SolicitudInvalidaException;

public class FechasCampaniaInvalidasException extends SolicitudInvalidaException {
    public FechasCampaniaInvalidasException(String mensaje) {
        super(mensaje);
    }
}
