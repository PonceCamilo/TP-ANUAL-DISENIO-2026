package ar.utn.donatrack.notificaciones.listener;

import ar.utn.donatrack.notificaciones.config.RabbitMQConfig;
import ar.utn.donatrack.notificaciones.dto.SolicitudNotificacionDto;
import ar.utn.donatrack.notificaciones.interfaces.services.NotificacionServiceInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Punto de entrada asincrónico (Entrega 4): en vez de exponerse solo vía
 * HTTP, este servicio también escucha la cola de RabbitMQ. Reutiliza el
 * mismo NotificacionService que usa el controller REST — el envío en sí
 * (Strategy/Factory/Template Method) no sabe ni le importa si el pedido
 * llegó por HTTP o por una cola.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificacionListener {

    private final NotificacionServiceInterface notificacionService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void recibir(SolicitudNotificacionDto solicitud) {
        log.info("Notificación recibida desde la cola para: {}", solicitud.destinatario());
        notificacionService.enviar(solicitud);
    }
}
