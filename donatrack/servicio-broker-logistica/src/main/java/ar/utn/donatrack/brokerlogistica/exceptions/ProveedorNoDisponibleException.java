package ar.utn.donatrack.brokerlogistica.exceptions;

/** Un proveedor puntual no respondió o falló al pedirle los envíos. */
public class ProveedorNoDisponibleException extends RuntimeException {
    public ProveedorNoDisponibleException(String proveedor, Throwable causa) {
        super("El proveedor de logística " + proveedor + " no está disponible", causa);
    }
}
