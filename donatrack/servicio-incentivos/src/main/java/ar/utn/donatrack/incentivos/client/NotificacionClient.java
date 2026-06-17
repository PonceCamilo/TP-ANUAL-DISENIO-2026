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
* Cliente HTTP que llama al servicio-notificaciones (puerto 8082).
* servicio-incentivos → POST /api/notificaciones/enviar → servicio-notificaciones
*
* Si el servicio de notificaciones no está levantado, loguea el error
* pero no frena el flujo de incentivos.
*/

@Component
public class NotificacionClient {

    private static final Logger log = LoggerFactory.getLogger(NotificacionClient.class);
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public NotificacionClient(
            @Value("${servicios.notificaciones.url:http://localhost:8082}") String baseUrl,
            ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient(); 
    }

/**
 * Envía una notificación al donante por su medio predeterminado.
 *
 * @param destinatario  email, teléfono o número WA del donante
 * @param mensaje       texto de la notificación
 * @param medio         "EMAIL", "SMS" o "WHATSAPP"
 */

    public void enviarNotificacion(String destinatario, String mensaje, String medio) {
        try {
            // 1. Armamos el payload y lo convertimos a JSON
            Map<String, String> payload = Map.of(
                    "destinatario", destinatario,
                    "mensaje", mensaje,
                    "medio", medio
            );
            String jsonBody = objectMapper.writeValueAsString(payload);

            // 2. Construimos la petición
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/notificaciones/enviar"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            // 3. Enviamos
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());

            log.info("[NotificacionClient] Notificación enviada a {} por {}", destinatario, medio);

        } catch (Exception e) {
            log.error("[NotificacionClient] No se pudo enviar notificación a {}: {}", destinatario, e.getMessage());
        }
    }
}