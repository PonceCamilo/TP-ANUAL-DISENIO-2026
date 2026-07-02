package ar.utn.donatrack.incentivos.exception;

public class CategoriasDonadasInvalidasException extends RuntimeException {

    public CategoriasDonadasInvalidasException() {
        super("Debe indicar al menos una categoria donada valida.");
    }
}
