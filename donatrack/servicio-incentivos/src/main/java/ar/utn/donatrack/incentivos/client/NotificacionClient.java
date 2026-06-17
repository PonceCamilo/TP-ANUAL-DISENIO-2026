package ar.utn.donatrack.incentivos.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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

    private final RestClient restClient;

    public NotificacionClient(
            @Value("${servicios.notificaciones.url:http://localhost:8082}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
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
            restClient.post()
                    .uri("/api/notificaciones/enviar")
                    .body(Map.of(
                            "destinatario", destinatario,
                            "mensaje", mensaje,
                            "medio", medio
                    ))
                    .retrieve()
                    .toBodilessEntity();

            log.info("[NotificacionClient] Notificación enviada a {} por {}", destinatario, medio);

        } catch (Exception e) {
            // No interrumpimos el flujo de incentivos si el servicio de notificaciones falla
            log.error("[NotificacionClient] No se pudo enviar notificación a {}: {}", destinatario, e.getMessage());
        }
    }
}
