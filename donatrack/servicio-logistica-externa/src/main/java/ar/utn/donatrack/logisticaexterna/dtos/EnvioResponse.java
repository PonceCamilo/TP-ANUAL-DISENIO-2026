package ar.utn.donatrack.logisticaexterna.dtos;

import ar.utn.donatrack.logisticaexterna.models.CambioEstadoEnvio;
import ar.utn.donatrack.logisticaexterna.models.Envio;

import java.time.LocalDate;
import java.util.List;

public record EnvioResponse(
        String trackingId,
        String shipmentRef,
        String destinatario,
        String domicilio,
        String estado,
        String vehiculo,
        String motivoFallo,
        String trackingUrl,
        LocalDate fechaEstimadaEntrega,
        List<CambioEstadoEnvio> historial) {

    public static EnvioResponse desde(Envio envio, String trackingUrl) {
        return new EnvioResponse(
                envio.getTrackingId(),
                envio.getShipmentRef(),
                envio.getDestinatario(),
                envio.getDomicilio(),
                envio.getEstado().name(),
                envio.getVehiculo(),
                envio.getMotivoFallo(),
                trackingUrl,
                envio.getFechaEstimadaEntrega(),
                List.copyOf(envio.getHistorial()));
    }
}
