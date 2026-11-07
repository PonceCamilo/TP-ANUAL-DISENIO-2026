package ar.utn.donatrack.donaciones.exceptions.donacionesExceptions;

import ar.utn.donatrack.donaciones.exceptions.NoEncontradoException;

import java.util.UUID;

/**
 * La donación no tiene un bien con ese id.
 */
public class BienNoEncontradoException extends NoEncontradoException {

    public BienNoEncontradoException(UUID idDonacion, UUID idBien) {
        super("La donación " + idDonacion + " no tiene un bien con id " + idBien + ".");
    }
}
