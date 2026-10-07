package ar.utn.donatrack.brokerlogistica.exceptions;

/** Donaciones no aceptó (o no respondió) el callback que le reenvió el broker. */
public class DonacionesNoDisponibleException extends RuntimeException {
    public DonacionesNoDisponibleException(String endpoint, Throwable causa) {
        super("No se pudo reenviar el evento a Donaciones (" + endpoint + "): " + causa.getMessage(), causa);
    }
}
