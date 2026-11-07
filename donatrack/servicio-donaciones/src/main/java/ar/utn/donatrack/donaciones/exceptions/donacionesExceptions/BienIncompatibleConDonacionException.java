package ar.utn.donatrack.donaciones.exceptions.donacionesExceptions;

import ar.utn.donatrack.donaciones.exceptions.ReglaDeNegocioException;

import java.util.UUID;

/**
 * El bien modificado ya no encaja en la donación: cambió de subcategoría, o de
 * vencimiento o de estado respecto de los otros bienes con los que fue segmentado.
 * Una donación tiene una sola subcategoría y la segmentación se haría distinto.
 */
public class BienIncompatibleConDonacionException extends ReglaDeNegocioException {

    public BienIncompatibleConDonacionException(String motivo) {
        super("El bien no corresponde a esta donación: " + motivo + " Registralo como una donación nueva.");
    }
}
