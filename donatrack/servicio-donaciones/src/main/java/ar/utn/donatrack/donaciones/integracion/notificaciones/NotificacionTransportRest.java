package ar.utn.donatrack.donaciones.integracion.notificaciones;

import ar.utn.donatrack.donaciones.interfaces.integracion.NotificacionTransport;
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
 * Si notificaciones no está levantado, la excepción sale hacia RelayDeMensajes,
 * que reintenta el aviso más tarde. El flujo de negocio ya no se entera: el aviso
 * se guardó en la bandeja de salida y se envía fuera de la operación.
 */
@Component
@ConditionalOnProperty(name = "notificaciones.transporte", havingValue = "rest", matchIfMissing = true)
public class NotificacionTransportRest implements NotificacionTransport {

    private final RestClient restClient;

    public NotificacionTransportRest(@Value("${notificaciones.url:http://localhost:8084}") String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    public void enviar(SolicitudNotificacionMensaje solicitud) {
        restClient.post()
                .uri("/notificaciones")
                .body(solicitud)
                .retrieve()
                .toBodilessEntity();
    }

    public String nombre() {
        return "REST (sincrónico)";
    }
}
