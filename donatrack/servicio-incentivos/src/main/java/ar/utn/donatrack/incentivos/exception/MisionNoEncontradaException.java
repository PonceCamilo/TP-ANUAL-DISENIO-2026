package ar.utn.donatrack.incentivos.exception;

public class MisionNoEncontradaException extends RuntimeException {

    public MisionNoEncontradaException(String categoria) {
        super("No se encontraron misiones para la categoria " + categoria + ".");
    }
}
