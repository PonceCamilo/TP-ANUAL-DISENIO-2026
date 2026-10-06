package ar.utn.donatrack.donaciones.integracion.notificaciones;

import ar.utn.donatrack.donaciones.config.RabbitMQConfig;
import ar.utn.donatrack.donaciones.interfaces.integracion.NotificacionTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Transporte asincrónico: publica la solicitud en la cola de RabbitMQ y vuelve
 * de inmediato, sin esperar a que el Servicio de Notificaciones la procese.
 *
 * Es lo que pide la Entrega 4: "La integración entre los servicios de dominio y
 * el Servicio de Notificaciones deberá realizarse de forma asincrónica, a través
 * de una cola de mensajes, a fin de no afectar la disponibilidad del sistema
 * ante picos de carga o fallas transitorias."
 *
 * Qué cambia respecto del transporte REST:
 *   - Si notificaciones está caído, los mensajes se acumulan en la cola y se
 *     procesan cuando vuelve. Antes se perdían.
 *   - El tiempo de respuesta de registrar una donación deja de depender de
 *     cuánto tarde notificaciones.
 *
 * Se mantiene el try/catch: si lo que está caído es el broker, el flujo de
 * negocio tampoco debe cortarse. Es el mismo criterio que ya se usaba con HTTP.
 */
@Component
@ConditionalOnProperty(name = "notificaciones.transporte", havingValue = "amqp")
public class NotificacionTransportAmqp implements NotificacionTransport {

    private static final Logger log = LoggerFactory.getLogger(NotificacionTransportAmqp.class);

    private final RabbitTemplate rabbitTemplate;

    public NotificacionTransportAmqp(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

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

    public String nombre() {
        return "AMQP/RabbitMQ (asincrónico)";
    }
}
