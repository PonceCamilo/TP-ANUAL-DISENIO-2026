package ar.utn.donatrack.logistica.exceptions;

public class PatenteDuplicadaException extends RuntimeException {
    public PatenteDuplicadaException(String patente) {
        super("Ya existe un camión con patente " + patente);
    }
}
