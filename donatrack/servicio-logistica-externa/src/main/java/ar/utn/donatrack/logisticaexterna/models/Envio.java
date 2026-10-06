package ar.utn.donatrack.logisticaexterna.models;

import ar.utn.donatrack.logisticaexterna.exceptions.TransicionEnvioIlegalException;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Un envío de este proveedor. shipmentRef es la referencia del cliente
 * (para Donatrack, el id de la donación); trackingId es el id propio.
 */
@Getter
public class Envio {

    private final String trackingId;
    private final String shipmentRef;
    private final String destinatario;
    private final String domicilio;
    private final LocalDate fechaEstimadaEntrega;
    private final List<CambioEstadoEnvio> historial = new ArrayList<>();

    private EstadoEnvio estado;
    private String vehiculo;
    private String motivoFallo;

    public Envio(String trackingId, String shipmentRef, String destinatario, String domicilio, LocalDate fechaEstimadaEntrega) {
        this.trackingId = trackingId;
        this.shipmentRef = shipmentRef;
        this.destinatario = destinatario;
        this.domicilio = domicilio;
        this.fechaEstimadaEntrega = fechaEstimadaEntrega;
        this.estado = EstadoEnvio.ACEPTADO;
        historial.add(new CambioEstadoEnvio(EstadoEnvio.ACEPTADO, LocalDateTime.now(), "Envío aceptado"));
    }

    public void despachar(String vehiculo) {
        cambiarA(EstadoEnvio.EN_CAMINO, "Despachado en vehículo " + vehiculo);
        this.vehiculo = vehiculo;
        this.motivoFallo = null;
    }

    public void entregar() {
        cambiarA(EstadoEnvio.ENTREGADO, "Entregado al destinatario");
    }

    public void fallar(String motivo) {
        cambiarA(EstadoEnvio.FALLIDO, motivo);
        this.motivoFallo = motivo;
    }

    public LocalDateTime fechaUltimoCambio() {
        return historial.get(historial.size() - 1).fecha();
    }

    private void cambiarA(EstadoEnvio nuevo, String detalle) {
        if (!estado.puedePasarA(nuevo)) {
            throw new TransicionEnvioIlegalException(trackingId, estado, nuevo);
        }
        estado = nuevo;
        historial.add(new CambioEstadoEnvio(nuevo, LocalDateTime.now(), detalle));
    }
}
