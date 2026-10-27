package ar.utn.donatrack.donaciones.exceptions.personasExceptions;

import java.util.UUID;

/**
 * No se puede borrar una persona donante que tiene donaciones registradas: la
 * donación la referencia por id y quedaría apuntando a nadie. Se responde 409.
 */
public class PersonaConDonacionesException extends RuntimeException {

    public PersonaConDonacionesException(UUID id) {
        super("La persona donante " + id + " tiene donaciones registradas y no se puede eliminar.");
    }
}
