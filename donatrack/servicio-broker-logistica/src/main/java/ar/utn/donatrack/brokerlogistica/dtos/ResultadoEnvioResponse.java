package ar.utn.donatrack.brokerlogistica.dtos;

import java.util.List;

/**
 * proveedoresDescartados: los que se probaron antes y no estaban disponibles
 * (vacío si atendió el primero). Sirve para ver el fallback en acción.
 */
public record ResultadoEnvioResponse(
        String proveedor,
        List<String> proveedoresDescartados,
        List<EnvioAsignadoDTO> envios) {
}
