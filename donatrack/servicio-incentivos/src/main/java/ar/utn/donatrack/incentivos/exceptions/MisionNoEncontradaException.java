package ar.utn.donatrack.incentivos.exceptions;

public class MisionNoEncontradaException extends RuntimeException {

    public MisionNoEncontradaException(String categoria) {
        super("No se encontraron misiones para la categoria " + categoria + ".");
    }
}
