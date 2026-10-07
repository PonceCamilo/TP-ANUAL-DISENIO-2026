package ar.utn.donatrack.donaciones.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topología de la cola de notificaciones (Entrega 4).
 *
 *   donaciones ──publish──▶ [exchange donatrack.notificaciones]
 *                                    │ routing key: notificacion.enviar
 *                                    ▼
 *                           [queue notificaciones.pendientes] ──▶ servicio-notificaciones
 *
 * POR QUÉ SE DECLARA DESDE ACÁ: lo habitual es que la cola la declare quien
 * consume. Se declara también del lado productor a propósito, para que la cola
 * exista aunque el Servicio de Notificaciones todavía no haya arrancado nunca.
 * Así los mensajes se acumulan en lugar de perderse, que es justamente la
 * resiliencia que pide el enunciado ("no afectar la disponibilidad del sistema
 * ante picos de carga o fallas transitorias"). Las declaraciones de AMQP son
 * idempotentes: que el consumidor declare lo mismo no genera conflicto.
 *
 * Toda la configuración es condicional: si el transporte es REST, estos beans
 * no se crean y el servicio no necesita un RabbitMQ corriendo.
 */
@Configuration
@ConditionalOnProperty(name = "notificaciones.transporte", havingValue = "amqp")
public class RabbitMQConfig {

    public static final String EXCHANGE = "donatrack.notificaciones";
    public static final String ROUTING_KEY = "notificacion.enviar";

    /**
     * Tiene que coincidir con la cola que escucha el NotificacionListener de
     * servicio-notificaciones. Si los nombres se desalinean, los mensajes se
     * acumulan en una cola que nadie lee y nada falla a la vista.
     */
    public static final String QUEUE = "notificaciones.queue";

    /** Exchange durable: sobrevive al reinicio del broker. */
    @Bean
    public TopicExchange notificacionesExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    /**
     * Cola durable: los mensajes encolados sobreviven a un reinicio de RabbitMQ.
     * Sin esto, una caída del broker perdería las notificaciones pendientes.
     */
    @Bean
    public Queue notificacionesQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    @Bean
    public Binding notificacionesBinding(Queue notificacionesQueue, TopicExchange notificacionesExchange) {
        return BindingBuilder.bind(notificacionesQueue).to(notificacionesExchange).with(ROUTING_KEY);
    }

    /**
     * Serializa el mensaje como JSON en lugar del binario de Java.
     * Es lo que permite que el consumidor lo reciba como su propio
     * SolicitudNotificacionDto sin compartir clases entre servicios.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}
