package ar.utn.donatrack.incentivos.client;

import ar.utn.donatrack.incentivos.models.insignias.InsigniaObtenida;
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
import java.util.UUID;

/**
* Cliente HTTP que dispara el flujo de n8n cuando un donante obtiene una insignia.
* servicio-incentivos → POST {integraciones.n8n.webhook.url} → n8n
*
* El flujo de n8n se encarga de generar la imagen de la insignia y publicarla
* en la red social configurada, mencionando al destinatario.
*
* Si n8n no está levantado o el webhook falla, se loguea el error
* pero no frena el flujo de incentivos.
*/

@Component
public class N8nWebhookClient {

    private static final Logger log = LoggerFactory.getLogger(N8nWebhookClient.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String webhookUrl;

    public N8nWebhookClient(
            @Value("${integraciones.n8n.webhook.url:http://localhost:5678/webhook/insignia-obtenida}") String webhookUrl,
            ObjectMapper objectMapper) {
        this.webhookUrl = webhookUrl;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Notifica a n8n que un donante obtuvo una insignia, para que el flujo
     * genere la imagen correspondiente y la publique en la red social elegida.
     *
     * @param donanteId     id del donante que obtuvo la insignia
     * @param insigniaObtenida insignia recién otorgada
     * @param destinatario  usuario/contacto a mencionar en la publicación
     */
    public void notificarInsigniaObtenida(UUID donanteId, InsigniaObtenida insigniaObtenida, String destinatario) {
        try {
            Map<String, Object> payload = Map.of(
                    "donanteId", donanteId.toString(),
                    "insigniaId", insigniaObtenida.getInsignia().getId(),
                    "insigniaNombre", insigniaObtenida.getInsignia().getNombre(),
                    "insigniaImagen", insigniaObtenida.getInsignia().getImagen(),
                    "fechaObtencion", insigniaObtenida.getFechaObtencion().toString(),
                    "destinatario", destinatario
            );
            String jsonBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            httpClient.send(request, HttpResponse.BodyHandlers.discarding());

            log.info("[N8nWebhookClient] Disparado flujo de n8n para insignia {} del donante {}",
                    insigniaObtenida.getInsignia().getNombre(), donanteId);

        } catch (Exception e) {
            log.error("[N8nWebhookClient] No se pudo disparar el flujo de n8n para el donante {}: {}",
                    donanteId, e.getMessage());
        }
    }
}
