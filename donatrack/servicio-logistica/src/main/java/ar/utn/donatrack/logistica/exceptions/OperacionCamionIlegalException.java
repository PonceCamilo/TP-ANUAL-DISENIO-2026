package ar.utn.donatrack.logistica.exceptions;

public class OperacionCamionIlegalException extends RuntimeException {
    public OperacionCamionIlegalException(String mensaje) {
        super(mensaje);
    }
}
