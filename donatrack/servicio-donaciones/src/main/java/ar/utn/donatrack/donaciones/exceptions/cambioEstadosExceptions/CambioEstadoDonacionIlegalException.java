package ar.utn.donatrack.donaciones.exceptions.cambioEstadosExceptions;

public class CambioEstadoDonacionIlegalException extends RuntimeException {
    public CambioEstadoDonacionIlegalException(String estadoActual, String estadoNuevo) {
        super("Transición inválida: " + estadoActual + " → " + estadoNuevo);
    }
}
