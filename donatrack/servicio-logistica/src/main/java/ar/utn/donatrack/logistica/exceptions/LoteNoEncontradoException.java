package ar.utn.donatrack.logistica.exceptions;

import java.util.UUID;

public class LoteNoEncontradoException extends RuntimeException {
    public LoteNoEncontradoException(UUID id) {
        super("No existe un lote de planificación con id " + id);
    }
}
