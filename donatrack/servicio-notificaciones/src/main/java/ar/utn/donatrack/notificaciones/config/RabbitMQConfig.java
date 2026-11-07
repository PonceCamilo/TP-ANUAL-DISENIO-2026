package ar.utn.donatrack.notificaciones.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
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

    /**
     * SIN ESTE BEAN LA COLA NO FUNCIONA.
     *
     * Por defecto Spring AMQP usa SimpleMessageConverter, que serializa con el
     * mecanismo binario de Java. Ese formato exige que la MISMA clase exista en
     * los dos extremos, y entre microservicios eso es imposible: donaciones
     * publica un SolicitudNotificacionMensaje de su propio paquete, incentivos
     * publica el suyo, y acá solo existe SolicitudNotificacionDto.
     *
     * Con el conversor JSON el mensaje viaja como texto y cada servicio lo
     * deserializa a su propia clase, siempre que los nombres de campo coincidan
     * (destinatario, mensaje, medio). Es lo que permite que cada servicio
     * mantenga su propio modelo sin compartir clases.
     *
     * Sin él, el NotificacionListener tira MessageConversionException en cada
     * mensaje y ninguna notificación se procesa.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}