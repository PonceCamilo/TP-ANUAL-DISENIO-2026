package ar.utn.donatrack.logistica.exceptions;

import java.util.UUID;

public class RutaNoEncontradaException extends RuntimeException {
    public RutaNoEncontradaException(UUID id) {
        super("No existe una ruta con id " + id);
    }
}
