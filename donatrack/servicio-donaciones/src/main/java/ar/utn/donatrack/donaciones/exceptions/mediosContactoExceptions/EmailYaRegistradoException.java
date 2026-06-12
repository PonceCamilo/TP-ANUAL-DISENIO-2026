package ar.utn.donatrack.donaciones.exceptions.mediosContactoExceptions;

public class EmailYaRegistradoException extends RuntimeException {

    public EmailYaRegistradoException() {
        super("Ese mail ya se encuentra registrado en el sistema.");
    }
}
