package ar.utn.donatrack.donaciones.exceptions.donacionesExceptions;

import ar.utn.donatrack.donaciones.exceptions.ConflictoException;

import java.util.UUID;

public class DonacionSinBienesException extends ConflictoException {
    public DonacionSinBienesException(UUID idDonacion) {
        super("La donación " + idDonacion + " no tiene bienes para modificar.");
    }
}
