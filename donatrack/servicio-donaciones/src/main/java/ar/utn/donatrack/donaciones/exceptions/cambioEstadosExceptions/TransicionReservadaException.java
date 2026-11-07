package ar.utn.donatrack.donaciones.exceptions.cambioEstadosExceptions;

import ar.utn.donatrack.donaciones.exceptions.ReglaDeNegocioException;

/**
 * El estado pedido existe y la transición es válida, pero no se hace a mano:
 * tiene su propio caso de uso, que además de cambiar el estado registra los datos
 * que lo acompañan (por ejemplo, la entidad y la fecha de una asignación).
 */
public class TransicionReservadaException extends ReglaDeNegocioException {

    public TransicionReservadaException(String estado, String comoSeHace) {
        super("A " + estado + " no se pasa con un cambio de estado manual: " + comoSeHace);
    }
}
