package ar.utn.donatrack.logisticaexterna.exceptions;

import ar.utn.donatrack.logisticaexterna.models.EstadoEnvio;

public class TransicionEnvioIlegalException extends RuntimeException {
    public TransicionEnvioIlegalException(String trackingId, EstadoEnvio actual, EstadoEnvio nuevo) {
        super("El envío " + trackingId + " no puede pasar de " + actual + " a " + nuevo);
    }
}
