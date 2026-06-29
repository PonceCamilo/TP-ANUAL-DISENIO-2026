package ar.utn.donatrack.donaciones.exceptions.cambioEstadosExceptions;

public class FaltaJustificacionDonacionException extends RuntimeException {
    public FaltaJustificacionDonacionException() {
        super("Es necesaria una justificación para registrar una entrega fallida.");
    }
}
