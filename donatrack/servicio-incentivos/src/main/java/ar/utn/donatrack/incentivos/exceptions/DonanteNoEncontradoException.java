package ar.utn.donatrack.incentivos.exceptions;

import java.util.UUID;

public class DonanteNoEncontradoException extends RuntimeException {

    public DonanteNoEncontradoException(UUID id) {
        super("No se encontro el donante con id " + id + ".");
    }
}
