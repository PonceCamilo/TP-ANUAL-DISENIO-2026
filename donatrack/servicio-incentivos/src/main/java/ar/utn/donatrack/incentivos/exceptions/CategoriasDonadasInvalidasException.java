package ar.utn.donatrack.incentivos.exceptions;

public class CategoriasDonadasInvalidasException extends RuntimeException {

    public CategoriasDonadasInvalidasException() {
        super("Debe indicar al menos una categoria donada valida.");
    }
}
