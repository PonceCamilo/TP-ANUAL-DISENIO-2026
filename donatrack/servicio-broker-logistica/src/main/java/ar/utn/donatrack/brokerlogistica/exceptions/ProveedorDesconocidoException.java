package ar.utn.donatrack.brokerlogistica.exceptions;

import java.util.Collection;

public class ProveedorDesconocidoException extends RuntimeException {
    public ProveedorDesconocidoException(String proveedor, Collection<String> conocidos) {
        super("Proveedor de logística desconocido: " + proveedor + ". Disponibles: " + conocidos);
    }
}
