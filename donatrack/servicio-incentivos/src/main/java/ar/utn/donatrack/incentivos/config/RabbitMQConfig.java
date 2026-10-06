package ar.utn.donatrack.incentivos.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de RabbitMQ para el Servicio de Incentivos.
 * Define la misma cola "notificaciones.queue" que usa servicio-notificaciones
 * para consumir. Incentivos publica mensajes en esa cola en vez de llamar
 * directamente por HTTP, desacoplando temporalmente al emisor del receptor.
 */
@Configuration
public class RabbitMQConfig {

    public static final String QUEUE = "notificaciones.queue";

    @Bean
    public Queue notificacionesQueue() {
        return new Queue(QUEUE, true);
    }
}