package ar.utn.donatrack.logisticaexterna.exceptions;

public class EnvioNoEncontradoException extends RuntimeException {
    public EnvioNoEncontradoException(String trackingId) {
        super("Envío " + trackingId + " no encontrado");
    }
}
