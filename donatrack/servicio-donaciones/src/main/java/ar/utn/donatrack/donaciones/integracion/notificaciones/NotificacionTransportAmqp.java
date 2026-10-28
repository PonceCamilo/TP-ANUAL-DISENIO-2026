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
 * CONFIRMACIONES (Entrega 5): publica con publisher confirms y espera a que el
 * broker confirme que recibió el mensaje. Antes, si la publicación fallaba se
 * registraba en el log y el aviso se perdía. Ahora, si el broker no confirma (o
 * está caído), la excepción sale hacia RelayDeMensajes, que reintenta el aviso
 * guardado en la bandeja de salida. El flujo de negocio no se entera.
 *
 * Usa el RabbitTemplate que autoconfigura Spring Boot (con el conversor JSON de
 * RabbitMQConfig) en lugar de uno armado a mano.
 */
@Component
@ConditionalOnProperty(name = "notificaciones.transporte", havingValue = "amqp")
public class NotificacionTransportAmqp implements NotificacionTransport {

    private static final Logger log = LoggerFactory.getLogger(NotificacionTransportAmqp.class);

    /** Cuánto se espera la confirmación del broker antes de dar el envío por fallido. */
    static final long TIEMPO_CONFIRMACION_MS = 5000;

    private final RabbitTemplate rabbitTemplate;

    public NotificacionTransportAmqp(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void enviar(SolicitudNotificacionMensaje solicitud) {
        rabbitTemplate.invoke(operaciones -> {
            operaciones.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, solicitud);
            operaciones.waitForConfirmsOrDie(TIEMPO_CONFIRMACION_MS);
            return null;
        });
        log.debug("[Notificaciones/AMQP] Encolada notificación para {} por {}",
                solicitud.destinatario(), solicitud.medio());
    }

    public String nombre() {
        return "AMQP/RabbitMQ (asincrónico)";
    }
}
