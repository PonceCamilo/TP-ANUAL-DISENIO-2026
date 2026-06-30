package ar.utn.donatrack.donaciones.exceptions.donacionesExceptions;

import java.util.UUID;

public class DonacionSinBienesException extends RuntimeException {
    public DonacionSinBienesException(UUID idDonacion) {
        super("La donación " + idDonacion + " no tiene bienes para modificar.");
    }
}
