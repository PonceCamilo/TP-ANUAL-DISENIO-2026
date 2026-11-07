package ar.utn.donatrack.logistica.integracion;

import ar.utn.donatrack.logistica.eventos.EntregaEvento;
import ar.utn.donatrack.logistica.interfaces.integracion.EntregaEventListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Observer concreto: traduce un EntregaEvento en un webhook al broker de
 * logística (servicio-broker-logistica).
 *
 * Esto es lo que permite cumplir la restricción "logística no debe invocar
 * directamente a Donaciones ni a Notificaciones": logística solo le avisa al
 * broker que ocurrió un hecho. El broker lo reenvía al endpoint de Donaciones
 * que corresponde según `tipo`, y es Donaciones quien resuelve los contactos
 * y dispara las notificaciones (Diseño A).
 */
@Component
public class BrokerEventosListener implements EntregaEventListener {

    private static final Logger log = LoggerFactory.getLogger(BrokerEventosListener.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String webhookUrl;

    public BrokerEventosListener(
            @Value("${integraciones.broker.eventos.url}") String webhookUrl,
            ObjectMapper objectMapper) {
        this.webhookUrl = webhookUrl;
        this.objectMapper = objectMapper;
        // Java 25 HttpClient intenta HTTP/2 por defecto; se fuerza HTTP/1.1
        // (lo que usan curl/Postman) para no depender de la negociación con
        // el servidor del otro lado.
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    @Override
    public void onEvento(EntregaEvento evento) {
        try {
            // El broker reenvía este body tal cual al endpoint de Donaciones que
            // corresponda según `tipo`. Los nombres de campo coinciden con los
            // DTOs de Donaciones para no tener que transformar nada.
            Map<String, Object> payload = construirPayload(evento);

            String jsonBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            // Fire and forget de verdad: envío asíncrono para no bloquear el hilo
            // que atiende la petición. Marcar la entrega no debe quedar esperando
            // a que el broker responda (ni colgarse si está caído o lento).
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .whenComplete((respuesta, error) -> {
                        if (error != null) {
                            log.error("[BrokerEventosListener] No se pudo disparar el evento {} para la entrega {}: {}",
                                    evento.getTipo(), evento.getEntregaId(), error.getMessage());
                        } else {
                            log.info("[BrokerEventosListener] Evento {} disparado para entrega {}",
                                    evento.getTipo(), evento.getEntregaId());
                        }
                    });
        } catch (Exception e) {
            log.error("[BrokerEventosListener] No se pudo preparar el evento {} para la entrega {}: {}",
                    evento.getTipo(), evento.getEntregaId(), e.getMessage());
        }
    }

    private Map<String, Object> construirPayload(EntregaEvento evento) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("tipo", evento.getTipo().name());

        switch (evento.getTipo()) {
            case INICIO_RUTA -> {
                payload.put("idRuta", evento.getRutaId());
                payload.put("idsDonaciones", evento.getIdsDonaciones());
                payload.put("urlMapaInteractivo", evento.getUrlMapaInteractivo());
            }
            case ENTREGA_CONFIRMADA -> {
                payload.put("rutaId", evento.getRutaId());
                payload.put("idDonacion", evento.getIdDonacion());
                payload.put("idCamion", evento.getIdCamion());
                payload.put("patenteCamion", evento.getPatenteCamion());
                payload.put("fechaHoraEntrega", evento.getFechaHoraEntrega());
            }
            case ENTREGA_NO_RECIBIDA -> {
                payload.put("rutaId", evento.getRutaId());
                payload.put("idDonacion", evento.getIdDonacion());
                payload.put("idCamion", evento.getIdCamion());
                payload.put("patenteCamion", evento.getPatenteCamion());
                payload.put("motivoFallo", evento.getMotivoFallo());
                payload.put("replanificable", evento.getReplanificable());
            }
        }

        return payload;
    }
}
