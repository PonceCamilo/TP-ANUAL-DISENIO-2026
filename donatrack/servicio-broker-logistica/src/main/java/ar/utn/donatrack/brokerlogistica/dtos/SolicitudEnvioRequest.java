package ar.utn.donatrack.brokerlogistica.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Pedido de envío que hace Donaciones al broker.
 *
 * proveedor es opcional: si se indica (ej. "EXTERNA") se usa solo ese; si no,
 * el broker elige según la prioridad configurada y hace fallback al siguiente
 * si el elegido no está disponible.
 */
public record SolicitudEnvioRequest(
        String proveedor,
        @NotEmpty @Valid List<DonacionEnvioDTO> donaciones) {
}
