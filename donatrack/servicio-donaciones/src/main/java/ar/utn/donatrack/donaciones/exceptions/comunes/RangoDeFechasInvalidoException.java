package ar.utn.donatrack.donaciones.exceptions.comunes;

import ar.utn.donatrack.donaciones.exceptions.SolicitudInvalidaException;

/** Se pidió un rango de fechas con el inicio posterior al fin. Se responde 400. */
public class RangoDeFechasInvalidoException extends SolicitudInvalidaException {

    public RangoDeFechasInvalidoException(String mensaje) {
        super(mensaje);
    }
}
