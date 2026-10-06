package ar.utn.donatrack.notificaciones.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de RabbitMQ para el Servicio de Notificaciones.
 *
 * Define la cola "notificaciones.queue" donde otros servicios (Donaciones,
 * Incentivos) publican sus solicitudes de notificación de forma asíncrona.
 * El NotificacionListener escucha esta cola y procesa cada mensaje
 * llamando al NotificacionService, sin que el emisor tenga que esperar
 * respuesta — desacoplando temporalmente al productor del consumidor.
 *
 * El parámetro "true" en la construcción de la Queue indica que es
 * durable: sobrevive a reinicios del broker RabbitMQ sin perder mensajes.
 */
@Configuration
public class RabbitMQConfig {

    public static final String QUEUE = "notificaciones.queue";

    @Bean
    public Queue notificacionesQueue() {
        return new Queue(QUEUE, true);
    }
}