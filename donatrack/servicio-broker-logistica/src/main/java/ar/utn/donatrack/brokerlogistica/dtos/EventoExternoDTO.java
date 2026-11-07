package ar.utn.donatrack.brokerlogistica.dtos;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

/**
 * Webhook de servicio-logistica-externa (su formato propio).
 * shipmentRef es el id de la donación que le mandó el broker.
 */
public record EventoExternoDTO(
        @NotBlank String trackingId,
        @NotBlank String shipmentRef,
        @NotBlank String status,
        String vehiculo,
        String motivo,
        String trackingUrl,
        LocalDateTime ocurridoEn) {
}
