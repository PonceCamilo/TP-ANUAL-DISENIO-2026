package ar.utn.donatrack.donaciones.clientes;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class NotificacionClient {

    private final RestClient restClient;

    public NotificacionClient(@Value("${notificaciones.url:http://localhost:8084}") String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    public void enviarNotificacion(String destinatario, String mensaje, String medio, String evento) {
        SolicitudNotificacionRequest body =
                new SolicitudNotificacionRequest(destinatario, mensaje, medio, evento);

        restClient.post()
                .uri("/notificaciones")
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    private record SolicitudNotificacionRequest(
            String destinatario, String mensaje, String medio, String evento) {}
}