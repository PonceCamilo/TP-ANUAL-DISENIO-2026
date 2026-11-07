package ar.utn.donatrack.donaciones.exceptions.personasExceptions;

import ar.utn.donatrack.donaciones.exceptions.ReglaDeNegocioException;

import java.util.UUID;

/** Una persona donante BLOQUEADA no puede registrar donaciones hasta que se la reactive. */
public class DonanteBloqueadoException extends ReglaDeNegocioException {

    public DonanteBloqueadoException(UUID idDonante) {
        super("La persona donante " + idDonante + " está BLOQUEADA y no puede registrar donaciones.");
    }
}
