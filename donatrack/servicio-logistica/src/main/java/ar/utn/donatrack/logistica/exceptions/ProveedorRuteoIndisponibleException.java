package ar.utn.donatrack.logistica.exceptions;

import java.util.UUID;

/** Se lanza cuando el proveedor externo de ruteo no responde, o responde un cuerpo que no se puede usar. */
public class ProveedorRuteoIndisponibleException extends RuntimeException {
    public ProveedorRuteoIndisponibleException(UUID camionId, Throwable causa) {
        super("El proveedor de ruteo no pudo planificar el camión " + camionId, causa);
    }

    public ProveedorRuteoIndisponibleException(String mensaje) {
        super(mensaje);
    }

    public ProveedorRuteoIndisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
