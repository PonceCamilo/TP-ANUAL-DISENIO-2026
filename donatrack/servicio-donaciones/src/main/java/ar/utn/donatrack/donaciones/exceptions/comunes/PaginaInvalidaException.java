package ar.utn.donatrack.donaciones.exceptions.comunes;

import ar.utn.donatrack.donaciones.exceptions.SolicitudInvalidaException;

/** Se pidió una página fuera de rango (número negativo o tamaño fuera de 1..100). Se responde 400. */
public class PaginaInvalidaException extends SolicitudInvalidaException {

    public PaginaInvalidaException(String mensaje) {
        super(mensaje);
    }
}
