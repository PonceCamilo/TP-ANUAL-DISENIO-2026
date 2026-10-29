package ar.utn.donatrack.donaciones.exceptions.cambioEstadosExceptions;

import ar.utn.donatrack.donaciones.exceptions.SolicitudInvalidaException;

/**
 * Una transición de estado que exige justificación llegó sin ella: bloquear a
 * una persona donante o registrar una entrega fallida.
 *
 * Antes había una por jerarquía de estados (donantes y donaciones) con el mismo
 * significado y el mismo código HTTP.
 */
public class FaltaJustificacionException extends SolicitudInvalidaException {

    public FaltaJustificacionException(String mensaje) {
        super(mensaje);
    }
}
