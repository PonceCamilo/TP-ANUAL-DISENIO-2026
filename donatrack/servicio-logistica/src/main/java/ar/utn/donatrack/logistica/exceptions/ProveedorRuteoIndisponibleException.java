package ar.utn.donatrack.logistica.exceptions;

import java.util.UUID;

/** Se lanza cuando el proveedor externo de ruteo no responde (o responde inválido) a la planificación síncrona de un camión. */
public class ProveedorRuteoIndisponibleException extends RuntimeException {
    public ProveedorRuteoIndisponibleException(UUID camionId, Throwable causa) {
        super("El proveedor de ruteo no pudo planificar el camión " + camionId, causa);
    }
}
