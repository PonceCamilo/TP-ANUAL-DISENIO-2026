package ar.utn.donatrack.donaciones.excepcion;

import java.util.UUID;

public class PersonaDonanteNoEncontradaException extends RuntimeException {

    public PersonaDonanteNoEncontradaException(UUID id) {
        super("No se encontró ninguna persona donante con el ID: " + id);
    }

    public PersonaDonanteNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}
