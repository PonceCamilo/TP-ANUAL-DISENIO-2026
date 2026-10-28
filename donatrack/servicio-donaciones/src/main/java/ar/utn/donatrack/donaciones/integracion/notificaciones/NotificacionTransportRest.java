package ar.utn.donatrack.donaciones.integracion.notificaciones;

import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.bind.annotation.RequestBody;
import ar.utn.donatrack.donaciones.config.ClientesHttp;
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

    /** El endpoint del Servicio de Notificaciones. */
    @HttpExchange
    public interface Api {

        @PostExchange("/notificaciones")
        void enviar(@RequestBody SolicitudNotificacionMensaje solicitud);
    }

    private final Api api;

    public NotificacionTransportRest(RestClient.Builder builder,
                                     @Value("${notificaciones.url:http://localhost:8084}") String baseUrl) {
        this.api = ClientesHttp.crear(builder, baseUrl, Api.class);
    }

    public void enviar(SolicitudNotificacionMensaje solicitud) {
        api.enviar(solicitud);
    }

    public String nombre() {
        return "REST (sincrónico)";
    }
}
