package ar.utn.donatrack.donaciones.exceptions.mediosContactoExceptions;

import ar.utn.donatrack.donaciones.exceptions.ConflictoException;

public class EmailYaRegistradoException extends ConflictoException {

    public EmailYaRegistradoException(String email) {
        super("El mail '" + email + "' ya se encuentra registrado en el sistema.");
    }
}
