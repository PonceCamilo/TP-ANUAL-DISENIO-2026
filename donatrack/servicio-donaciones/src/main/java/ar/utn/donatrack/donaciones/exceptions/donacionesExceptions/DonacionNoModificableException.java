package ar.utn.donatrack.donaciones.exceptions.donacionesExceptions;

import ar.utn.donatrack.donaciones.exceptions.ReglaDeNegocioException;

import java.util.UUID;

/**
 * Los bienes solo se corrigen mientras la donación está en el depósito: una vez
 * asignada, despachada o entregada, cambiar lo que contiene rompería la trazabilidad.
 */
public class DonacionNoModificableException extends ReglaDeNegocioException {

    public DonacionNoModificableException(UUID idDonacion, String estado) {
        super("La donación " + idDonacion + " está en " + estado + ": sus bienes solo se pueden modificar mientras está EN_DEPOSITO.");
    }
}
