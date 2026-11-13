package ar.utn.donatrack.donaciones.exceptions.donacionesExceptions;

import ar.utn.donatrack.donaciones.exceptions.ReglaDeNegocioException;

/**
 * Entrega 6: la división pedida no se puede hacer (cantidades fuera de rango,
 * bienes que no son de la donación, o una de las dos partes quedaría vacía).
 */
public class DivisionInvalidaException extends ReglaDeNegocioException {
    public DivisionInvalidaException(String motivo) {
        super(motivo);
    }
}
