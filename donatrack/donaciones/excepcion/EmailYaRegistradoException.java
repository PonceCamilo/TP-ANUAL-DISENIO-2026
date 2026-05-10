package com.donatrack.donaciones.excepcion;

public class EmailYaRegistradoException extends RuntimeException {

    public EmailYaRegistradoException(String email) {
        super("El email '" + email + "' ya se encuentra registrado en el sistema.");
    }
}
