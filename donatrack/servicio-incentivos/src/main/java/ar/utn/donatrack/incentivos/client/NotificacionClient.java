package ar.utn.donatrack.incentivos.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * Cliente HTTP que llama al servicio-notificaciones (puerto 8084).
 * servicio-incentivos → POST /notificaciones → servicio-notificaciones
 * Si el servicio de notificaciones no está levantado, loguea el error
 * pero NO frena el flujo de incentivos.
 */
@Component
public class NotificacionClient {

    private static final Logger log = LoggerFactory.getLogger(NotificacionClient.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public NotificacionClient(
            @Value("${servicios.notificaciones.url:http://localhost:8084}") String baseUrl,
            ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Envía una notificación al donante por el medio indicado.
     *
     * @param destinatario email, teléfono o número WA del donante
     * @param mensaje      texto de la notificación
     * @param medio        "EMAIL", "SMS" o "WHATSAPP"
     */
    public void enviarNotificacion(String destinatario, String mensaje, String medio) {
        try {
            Map<String, String> payload = Map.of(
                    "destinatario", destinatario,
                    "mensaje", mensaje,
                    "medio", medio

            );
            String jsonBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/notificaciones"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            httpClient.send(request, HttpResponse.BodyHandlers.discarding());

            log.info("[NotificacionClient] Notificación enviada a {} por {}", destinatario, medio);

        } catch (Exception e) {
            log.error("[NotificacionClient] No se pudo enviar notificación a {}: {}", destinatario, e.getMessage());
        }
    }
}
