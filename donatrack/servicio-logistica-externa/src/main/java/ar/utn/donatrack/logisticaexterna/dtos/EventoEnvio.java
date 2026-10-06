package ar.utn.donatrack.logisticaexterna.dtos;

import java.time.LocalDateTime;

/**
 * Payload del webhook que este proveedor envía en cada cambio de estado.
 * Es su formato propio: el broker lo traduce al que espera Donaciones.
 *
 * vehiculo solo viene desde EN_CAMINO; motivo solo en FALLIDO.
 */
public record EventoEnvio(
        String trackingId,
        String shipmentRef,
        String status,
        String vehiculo,
        String motivo,
        String trackingUrl,
        LocalDateTime ocurridoEn) {
}
