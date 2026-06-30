package ar.utn.donatrack.logistica.exceptions;

import java.util.UUID;

public class EntregaNoEncontradaException extends RuntimeException {
    public EntregaNoEncontradaException(UUID id) {
        super("No existe una entrega con id " + id);
    }
}
