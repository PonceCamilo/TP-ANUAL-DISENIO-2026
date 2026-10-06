package ar.utn.donatrack.donaciones.integracion.notificaciones;

import ar.utn.donatrack.donaciones.interfaces.integracion.NotificacionTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Transporte sincrónico: POST /notificaciones contra el servicio (puerto 8084).
 *
 * Es el comportamiento histórico y sigue siendo el predeterminado. La Entrega 4
 * pide reemplazarlo por la cola, pero se conserva porque:
 *   - permite volver atrás con una sola property si la cola da problemas;
 *   - sirve para comparar los dos enfoques en la defensa.
 *
 * Si notificaciones no está levantado, loguea el error pero NO corta el flujo
 * que lo invocó: una donación ya registrada no puede reportarse como fallida
 * porque el aviso posterior no se pudo enviar.
 */
@Component
@ConditionalOnProperty(name = "notificaciones.transporte", havingValue = "rest", matchIfMissing = true)
public class NotificacionTransportRest implements NotificacionTransport {

    private static final Logger log = LoggerFactory.getLogger(NotificacionTransportRest.class);

    private final RestClient restClient;

    public NotificacionTransportRest(@Value("${notificaciones.url:http://localhost:8084}") String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    public void enviar(SolicitudNotificacionMensaje solicitud) {
        try {
            restClient.post()
                    .uri("/notificaciones")
                    .body(solicitud)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("[Notificaciones/REST] Error al enviar notificación a {}: {}",
                    solicitud.destinatario(), e.getMessage());
        }
    }

    public String nombre() {
        return "REST (sincrónico)";
    }
}
