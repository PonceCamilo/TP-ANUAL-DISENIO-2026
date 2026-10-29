package ar.utn.donatrack.donaciones.exceptions.personasExceptions;

import ar.utn.donatrack.donaciones.exceptions.ConflictoException;

public class PersonaConMismoEstadoException extends ConflictoException {
    public PersonaConMismoEstadoException(String estado) {
        super("La persona donante ya se encuentra con el estado: " + estado);
    }
}
