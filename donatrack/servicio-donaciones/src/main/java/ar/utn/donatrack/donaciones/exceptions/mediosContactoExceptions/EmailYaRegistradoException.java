package ar.utn.donatrack.donaciones.exceptions.mediosContactoExceptions;

public class EmailYaRegistradoException extends RuntimeException {

    public EmailYaRegistradoException(String email) {
        super("El mail '" + email + "' ya se encuentra registrado en el sistema.");
    }
}
