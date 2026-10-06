package ar.utn.donatrack.logisticaexterna.integracion;

import ar.utn.donatrack.logisticaexterna.dtos.EventoEnvio;
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

/**
 * Avisa cada cambio de estado de un envío al webhook del cliente (el broker
 * de logística de Donatrack). Fire and forget: si el webhook no responde,
 * se loguea y el envío sigue su curso.
 */
@Component
public class WebhookEventosNotifier {

    private static final Logger log = LoggerFactory.getLogger(WebhookEventosNotifier.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String webhookUrl;

    public WebhookEventosNotifier(
            @Value("${logistica-externa.webhook.url:}") String webhookUrl,
            ObjectMapper objectMapper) {
        this.webhookUrl = webhookUrl;
        this.objectMapper = objectMapper;
        // HTTP/1.1 por el mismo motivo que BrokerEventosListener en servicio-logistica.
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public void notificar(EventoEnvio evento) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("[WebhookEventosNotifier] Sin webhook configurado; no se avisa el evento {} del envío {}",
                    evento.status(), evento.trackingId());
            return;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(evento)))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .whenComplete((respuesta, error) -> {
                        if (error != null) {
                            log.error("[WebhookEventosNotifier] No se pudo avisar el evento {} del envío {}: {}",
                                    evento.status(), evento.trackingId(), error.getMessage());
                        } else {
                            log.info("[WebhookEventosNotifier] Evento {} del envío {} avisado (HTTP {})",
                                    evento.status(), evento.trackingId(), respuesta.statusCode());
                        }
                    });
        } catch (Exception e) {
            log.error("[WebhookEventosNotifier] No se pudo preparar el evento {} del envío {}: {}",
                    evento.status(), evento.trackingId(), e.getMessage());
        }
    }
}
