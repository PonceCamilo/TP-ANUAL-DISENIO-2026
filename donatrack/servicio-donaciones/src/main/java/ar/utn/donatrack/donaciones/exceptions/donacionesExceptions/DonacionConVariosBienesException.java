package ar.utn.donatrack.donaciones.exceptions.donacionesExceptions;

import ar.utn.donatrack.donaciones.exceptions.ConflictoException;

import java.util.UUID;

/**
 * El endpoint que modifica «el» bien de una donación solo sirve cuando tiene uno solo:
 * con varios hay que indicar cuál.
 */
public class DonacionConVariosBienesException extends ConflictoException {

    public DonacionConVariosBienesException(UUID idDonacion, int cantidadBienes) {
        super("La donación " + idDonacion + " tiene " + cantidadBienes + " bienes: indicá cuál con PUT /donaciones/" + idDonacion + "/bienes/{idBien}.");
    }
}
