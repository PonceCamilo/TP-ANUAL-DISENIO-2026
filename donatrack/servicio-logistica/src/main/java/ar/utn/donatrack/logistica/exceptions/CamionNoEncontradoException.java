package ar.utn.donatrack.logistica.exceptions;

import java.util.UUID;

public class CamionNoEncontradoException extends RuntimeException {
    public CamionNoEncontradoException(UUID id) {
        super("No existe un camión con id " + id);
    }
}
