package ar.utn.donatrack.donaciones.clientes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP que llama al servicio-notificaciones (puerto 8084).
 * Si notificaciones no está levantado, loguea el error pero NO frena el flujo
 * que la invoca (igual criterio que IncentivosClient): una donación o
 * asignación que ya se persistió no debe reportarse como fallida solo porque
 * el aviso posterior no pudo enviarse.
 */
@Component
public class NotificacionClient {

    private static final Logger log = LoggerFactory.getLogger(NotificacionClient.class);

    private final RestClient restClient;

    public NotificacionClient(@Value("${notificaciones.url:http://localhost:8084}") String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    public void enviarNotificacion(String destinatario, String mensaje, String medio) {
        try {
            SolicitudNotificacionRequest body =
                    new SolicitudNotificacionRequest(destinatario, mensaje, medio);

            restClient.post()
                    .uri("/notificaciones")
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("[NotificacionClient] Error al enviar notificación a {}: {}", destinatario, e.getMessage());
        }
    }

    private record SolicitudNotificacionRequest(
            String destinatario, String mensaje, String medio) {}
}
