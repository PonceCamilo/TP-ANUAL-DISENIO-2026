package ar.utn.donatrack.donaciones.exceptions.personasExceptions;

import java.util.UUID;

public class PersonaDonanteNoEncontradaException extends RuntimeException {

    public PersonaDonanteNoEncontradaException(UUID id) {
        super("No se encontró ninguna persona donante con el ID: " + id);
    }
}
