package ar.utn.donatrack.incentivos.integracion.notificaciones;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Transporte sincrónico: hace un POST HTTP directo a servicio-notificaciones y espera respuesta.
 * Se activa cuando notificaciones.transporte=rest (o si no está configurado).
 */
@Component
@ConditionalOnProperty(
        name = "notificaciones.transporte",
        havingValue = "rest",
        matchIfMissing = true)
public class NotificacionTransportRest implements NotificacionTransport {

    private static final Logger log = LoggerFactory.getLogger(NotificacionTransportRest.class);

    private final RestClient restClient;
    private final String clave;

    /**
     * Entrega 6: Notificaciones acepta envíos por HTTP solo de los servicios de
     * DonaTrack, con la clave entre servicios (servicios.clave-servicios).
     */
    public NotificacionTransportRest(
            @Value("${servicios.notificaciones.url:http://localhost:8084}") String baseUrl,
            @Value("${servicios.clave-servicios:}") String clave) {
        this.restClient = RestClient.create(baseUrl);
        this.clave = clave == null ? "" : clave;
    }

    @Override
    public void enviar(SolicitudNotificacionMensaje solicitud) {
        try {
            restClient.post()
                    .uri("/notificaciones")
                    .headers(cabeceras -> {
                        if (!clave.isBlank()) {
                            cabeceras.set("X-DonaTrack-Servicio", clave);
                        }
                    })
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(solicitud)
                    .retrieve()
                    .toBodilessEntity();
            log.info("[Notificaciones/REST] Notificación enviada a {} por {}",
                    solicitud.destinatario(), solicitud.medio());
        } catch (Exception e) {
            log.error("[Notificaciones/REST] No se pudo enviar notificación a {}: {}",
                    solicitud.destinatario(), e.getMessage());
        }
    }

    @Override
    public String nombre() {
        return "REST/HTTP (sincrónico)";
    }
}