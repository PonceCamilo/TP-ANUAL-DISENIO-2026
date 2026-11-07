package ar.utn.donatrack.donaciones.exceptions.personasExceptions;

import ar.utn.donatrack.donaciones.exceptions.NoEncontradoException;

import java.util.UUID;

public class PersonaDonanteNoEncontradaException extends NoEncontradoException {

    public PersonaDonanteNoEncontradaException(UUID id) {
        super("No se encontró ninguna persona donante con el ID: " + id);
    }
}
