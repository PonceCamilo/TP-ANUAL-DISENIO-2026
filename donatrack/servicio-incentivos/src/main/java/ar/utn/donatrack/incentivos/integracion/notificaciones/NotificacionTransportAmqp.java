package ar.utn.donatrack.incentivos.integracion.notificaciones;

import ar.utn.donatrack.incentivos.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Transporte asincrónico: publica la solicitud en la cola de RabbitMQ
 * y vuelve de inmediato, sin esperar a que servicio-notificaciones la procese.
 *
 * Si notificaciones está caído, los mensajes se acumulan en la cola y se
 * procesan cuando vuelve. Antes con HTTP se perdían.
 */
@Component
@ConditionalOnProperty(name = "notificaciones.transporte", havingValue = "amqp")
public class NotificacionTransportAmqp implements NotificacionTransport {

    private static final Logger log = LoggerFactory.getLogger(NotificacionTransportAmqp.class);

    private final RabbitTemplate rabbitTemplate;

    public NotificacionTransportAmqp(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void enviar(SolicitudNotificacionMensaje solicitud) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE,
                    RabbitMQConfig.ROUTING_KEY,
                    solicitud);
            log.debug("[Notificaciones/AMQP] Encolada notificación para {} por {}",
                    solicitud.destinatario(), solicitud.medio());
        } catch (Exception e) {
            log.error("[Notificaciones/AMQP] No se pudo encolar la notificación para {}: {}",
                    solicitud.destinatario(), e.getMessage());
        }
    }

    @Override
    public String nombre() {
        return "AMQP/RabbitMQ (asincrónico)";
    }
}