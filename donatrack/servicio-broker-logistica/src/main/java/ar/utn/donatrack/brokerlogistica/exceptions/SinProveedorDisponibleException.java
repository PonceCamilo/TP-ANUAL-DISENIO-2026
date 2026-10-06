package ar.utn.donatrack.brokerlogistica.exceptions;

import java.util.List;

/** Ninguno de los proveedores candidatos pudo tomar el pedido. */
public class SinProveedorDisponibleException extends RuntimeException {
    public SinProveedorDisponibleException(List<String> probados) {
        super("Ningún proveedor de logística disponible. Probados: " + probados);
    }
}
