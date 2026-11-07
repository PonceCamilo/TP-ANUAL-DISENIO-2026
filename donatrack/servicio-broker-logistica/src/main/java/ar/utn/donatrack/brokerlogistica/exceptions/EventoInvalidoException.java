package ar.utn.donatrack.brokerlogistica.exceptions;

/** Un evento de un proveedor que el broker no sabe traducir. */
public class EventoInvalidoException extends RuntimeException {
    public EventoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
