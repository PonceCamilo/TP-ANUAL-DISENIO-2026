package ar.utn.donatrack.incentivos.client;

import ar.utn.donatrack.incentivos.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Publica solicitudes de notificación en la cola de RabbitMQ.
 * servicio-incentivos → cola RabbitMQ → servicio-notificaciones consume.
 *
 * Cambio respecto a la entrega anterior: en vez de llamar directamente
 * por HTTP (acoplamiento sincrónico), se publica el mensaje en la cola
 * de forma asíncrona. Si notificaciones está caído, el mensaje queda
 * en la cola y se procesa cuando vuelve — nadie pierde el aviso.
 */
@Component
public class NotificacionClient {

    private static final Logger log = LoggerFactory.getLogger(NotificacionClient.class);

    private final RabbitTemplate rabbitTemplate;

    public NotificacionClient(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void enviarNotificacion(String destinatario, String mensaje, String medio, String evento) {
        try {
            Map<String, String> payload = Map.of(
                    "destinatario", destinatario,
                    "mensaje", mensaje,
                    "medio", medio,
                    "evento", evento
            );

            rabbitTemplate.convertAndSend(RabbitMQConfig.QUEUE, payload);

            log.info("[NotificacionClient] Notificación publicada en cola para {} por {}", destinatario, medio);

        } catch (Exception e) {
            log.error("[NotificacionClient] No se pudo publicar notificación para {}: {}", destinatario, e.getMessage());
        }
    }
}