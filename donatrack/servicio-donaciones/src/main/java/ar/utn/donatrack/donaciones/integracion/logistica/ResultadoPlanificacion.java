package ar.utn.donatrack.donaciones.integracion.logistica;

import java.util.List;
import java.util.UUID;

/**
 * Qué pasó al intentar derivar un conjunto de donaciones a un proveedor de logística.
 *
 * No se lanza una excepción cuando falla: el proceso que dispara la planificación
 * es un batch nocturno y necesita poder registrar el fracaso y seguir, no cortarse.
 */
public record ResultadoPlanificacion(
        String proveedor,
        boolean aceptada,
        List<UUID> idsDonaciones,
        String detalle) {

    public static ResultadoPlanificacion aceptada(String proveedor, List<UUID> idsDonaciones) {
        return new ResultadoPlanificacion(proveedor, true, idsDonaciones,
                "El proveedor aceptó " + idsDonaciones.size() + " donaciones para planificar.");
    }

    public static ResultadoPlanificacion rechazada(String proveedor, String motivo) {
        return new ResultadoPlanificacion(proveedor, false, List.of(), motivo);
    }

    /** Ningún proveedor pudo hacerse cargo: las donaciones quedan para el próximo intento. */
    public static ResultadoPlanificacion sinProveedorDisponible() {
        return new ResultadoPlanificacion("ninguno", false, List.of(),
                "Ningún proveedor de logística estaba disponible.");
    }
}
