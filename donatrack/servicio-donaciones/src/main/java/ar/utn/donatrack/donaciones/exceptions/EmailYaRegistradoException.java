package ar.utn.donatrack.donaciones.exceptions;

public class EmailYaRegistradoException extends RuntimeException {

    public EmailYaRegistradoException() {
        super("Ese mail ya se encuentra registrado en el sistema.");
    }
}
