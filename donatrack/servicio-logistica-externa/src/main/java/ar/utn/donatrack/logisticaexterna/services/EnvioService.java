package ar.utn.donatrack.logisticaexterna.services;

import ar.utn.donatrack.logisticaexterna.dtos.CrearEnvioRequest;
import ar.utn.donatrack.logisticaexterna.dtos.EnvioResponse;
import ar.utn.donatrack.logisticaexterna.dtos.EventoEnvio;
import ar.utn.donatrack.logisticaexterna.exceptions.EnvioNoEncontradoException;
import ar.utn.donatrack.logisticaexterna.integracion.WebhookEventosNotifier;
import ar.utn.donatrack.logisticaexterna.models.Envio;
import ar.utn.donatrack.logisticaexterna.repositories.EnvioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@Service
public class EnvioService {

    private final EnvioRepository repositorio;
    private final WebhookEventosNotifier notifier;
    private final String baseTrackingUrl;
    private final int diasEstimadosEntrega;

    public EnvioService(
            EnvioRepository repositorio,
            WebhookEventosNotifier notifier,
            @Value("${logistica-externa.tracking-base-url:http://localhost:8086/api/v1/envios/}") String baseTrackingUrl,
            @Value("${logistica-externa.dias-estimados-entrega:2}") int diasEstimadosEntrega) {
        this.repositorio = repositorio;
        this.notifier = notifier;
        this.baseTrackingUrl = baseTrackingUrl;
        this.diasEstimadosEntrega = diasEstimadosEntrega;
    }

    /** El alta no dispara webhook: el cliente ya recibe el tracking en la respuesta. */
    public EnvioResponse crear(CrearEnvioRequest request) {
        Envio envio = new Envio(
                "EXT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                request.shipmentRef(),
                request.destinatario(),
                request.domicilio(),
                LocalDate.now().plusDays(diasEstimadosEntrega));
        repositorio.guardar(envio);
        return aResponse(envio);
    }

    public EnvioResponse consultar(String trackingId) {
        return aResponse(buscarOFallar(trackingId));
    }

    public List<EnvioResponse> listar() {
        return repositorio.buscarTodos().stream().map(this::aResponse).toList();
    }

    public EnvioResponse despachar(String trackingId, String vehiculo) {
        return transicionar(trackingId, envio -> envio.despachar(vehiculo));
    }

    public EnvioResponse entregar(String trackingId) {
        return transicionar(trackingId, Envio::entregar);
    }

    public EnvioResponse fallar(String trackingId, String motivo) {
        return transicionar(trackingId, envio -> envio.fallar(motivo));
    }

    private EnvioResponse transicionar(String trackingId, Consumer<Envio> transicion) {
        Envio envio = buscarOFallar(trackingId);
        transicion.accept(envio);
        repositorio.guardar(envio);
        notifier.notificar(aEvento(envio));
        return aResponse(envio);
    }

    private Envio buscarOFallar(String trackingId) {
        return repositorio.buscarPorTracking(trackingId)
                .orElseThrow(() -> new EnvioNoEncontradoException(trackingId));
    }

    private EventoEnvio aEvento(Envio envio) {
        return new EventoEnvio(
                envio.getTrackingId(),
                envio.getShipmentRef(),
                envio.getEstado().name(),
                envio.getVehiculo(),
                envio.getMotivoFallo(),
                trackingUrl(envio),
                envio.fechaUltimoCambio());
    }

    private EnvioResponse aResponse(Envio envio) {
        return EnvioResponse.desde(envio, trackingUrl(envio));
    }

    private String trackingUrl(Envio envio) {
        return baseTrackingUrl + envio.getTrackingId();
    }
}
