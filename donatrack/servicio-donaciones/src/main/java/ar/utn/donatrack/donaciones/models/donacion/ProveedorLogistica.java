package ar.utn.donatrack.donaciones.models.donacion;

import java.util.Arrays;
import java.util.Locale;

/**
 * Los proveedores de logística detrás del broker. Antes viajaban como texto
 * libre ("DONATRACK"/"EXTERNA") en el modelo, el DTO y el pedido.
 */
public enum ProveedorLogistica {
    DONATRACK,
    EXTERNA;

    /** El proveedor que informa el broker; uno desconocido es un error de contrato. */
    public static ProveedorLogistica desde(String nombre) {
        if (nombre != null) {
            String buscado = nombre.strip().toUpperCase(Locale.ROOT);
            for (ProveedorLogistica proveedor : values()) {
                if (proveedor.name().equals(buscado)) {
                    return proveedor;
                }
            }
        }
        throw new IllegalArgumentException("Proveedor de logística desconocido: " + nombre
                + ". Se esperaba uno de " + Arrays.toString(values()) + ".");
    }
}
