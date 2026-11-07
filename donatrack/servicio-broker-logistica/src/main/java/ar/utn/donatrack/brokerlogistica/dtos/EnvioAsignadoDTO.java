package ar.utn.donatrack.brokerlogistica.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Qué proveedor tomó una donación y con qué identificador la sigue él
 * (id de entrega en servicio-logistica, trackingId en el externo).
 */
public record EnvioAsignadoDTO(
        UUID idDonacion,
        String proveedor,
        String idSeguimiento,
        LocalDateTime fechaAsignacion) {
}
