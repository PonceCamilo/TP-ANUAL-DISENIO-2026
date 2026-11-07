package ar.utn.donatrack.brokerlogistica.services;

import ar.utn.donatrack.brokerlogistica.clientes.DonacionesClient;
import ar.utn.donatrack.brokerlogistica.dtos.EventoExternoDTO;
import ar.utn.donatrack.brokerlogistica.dtos.donaciones.EntregaExitosaCallback;
import ar.utn.donatrack.brokerlogistica.dtos.donaciones.EntregaFallidaCallback;
import ar.utn.donatrack.brokerlogistica.dtos.donaciones.InicioRutaCallback;
import ar.utn.donatrack.brokerlogistica.exceptions.EventoInvalidoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Vuelta del broker: recibe los eventos de cada proveedor con su formato y
 * los reenvía a Donaciones con el formato de sus callbacks. Donaciones no se
 * entera de qué proveedor hizo la entrega.
 */
@Service
public class BrokerEventosService {

    private static final Logger log = LoggerFactory.getLogger(BrokerEventosService.class);

    // Lo que el proveedor externo no reporta se completa con estos valores.
    private static final String MOTIVO_SIN_INFORMAR = "Motivo no informado por el proveedor";
    private static final String VEHICULO_SIN_INFORMAR = "SIN-PATENTE";

    private final DonacionesClient donacionesClient;

    public BrokerEventosService(DonacionesClient donacionesClient) {
        this.donacionesClient = donacionesClient;
    }

    /**
     * servicio-logistica ya publica con los nombres de campo de Donaciones:
     * solo hay que enrutar por `tipo` (lo mismo que hacía el workflow de n8n).
     */
    public void procesarEventoDonatrack(Map<String, Object> evento) {
        Object tipo = evento.get("tipo");
        String endpoint = switch (String.valueOf(tipo)) {
            case "INICIO_RUTA" -> DonacionesClient.INICIO_RUTA;
            case "ENTREGA_CONFIRMADA" -> DonacionesClient.ENTREGA_EXITOSA;
            case "ENTREGA_NO_RECIBIDA" -> DonacionesClient.ENTREGA_FALLIDA;
            default -> throw new EventoInvalidoException("Tipo de evento de DONATRACK desconocido: " + tipo);
        };
        log.info("[Broker] Evento {} de DONATRACK", tipo);
        donacionesClient.reenviar(endpoint, evento);
    }

    /**
     * El externo avisa por envío (una donación) con su propio vocabulario.
     * Su modelo no tiene rutas ni camiones con id: se derivan ids estables a
     * partir del trackingId y del vehículo para cumplir el contrato de
     * Donaciones. FALLIDO siempre es replanificable (el externo permite
     * volver a despachar).
     *
     * @return false si el evento no le interesa a Donaciones (ej. ACEPTADO).
     */
    public boolean procesarEventoExterno(EventoExternoDTO evento) {
        UUID idDonacion = idDonacion(evento.shipmentRef());
        log.info("[Broker] Evento {} de EXTERNA para donación {}", evento.status(), idDonacion);

        switch (evento.status()) {
            case "ACEPTADO" -> {
                return false;
            }
            case "EN_CAMINO" -> donacionesClient.reenviar(DonacionesClient.INICIO_RUTA, new InicioRutaCallback(
                    uuidDerivado("ruta-externa:" + evento.trackingId()),
                    List.of(idDonacion),
                    evento.trackingUrl()));
            case "ENTREGADO" -> {
                String vehiculo = evento.vehiculo() != null ? evento.vehiculo() : VEHICULO_SIN_INFORMAR;
                donacionesClient.reenviar(DonacionesClient.ENTREGA_EXITOSA, new EntregaExitosaCallback(
                        idDonacion,
                        uuidDerivado("vehiculo-externo:" + vehiculo),
                        vehiculo,
                        evento.ocurridoEn() != null ? evento.ocurridoEn() : LocalDateTime.now()));
            }
            case "FALLIDO" -> donacionesClient.reenviar(DonacionesClient.ENTREGA_FALLIDA, new EntregaFallidaCallback(
                    idDonacion,
                    evento.motivo() != null && !evento.motivo().isBlank() ? evento.motivo() : MOTIVO_SIN_INFORMAR,
                    true));
            default -> throw new EventoInvalidoException("Estado de EXTERNA desconocido: " + evento.status());
        }
        return true;
    }

    private UUID idDonacion(String shipmentRef) {
        try {
            return UUID.fromString(shipmentRef);
        } catch (IllegalArgumentException e) {
            throw new EventoInvalidoException("shipmentRef no es un id de donación: " + shipmentRef);
        }
    }

    private UUID uuidDerivado(String clave) {
        return UUID.nameUUIDFromBytes(clave.getBytes(StandardCharsets.UTF_8));
    }
}
