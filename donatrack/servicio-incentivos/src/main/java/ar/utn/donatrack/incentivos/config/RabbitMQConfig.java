package ar.utn.donatrack.incentivos.config;

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

@Configuration
@ConditionalOnProperty(name = "notificaciones.transporte", havingValue = "amqp") // esta config es solo para cuando se usa AMQP como transporte de notificaciones
public class RabbitMQConfig {

    public static final String EXCHANGE = "donatrack.notificaciones";
    public static final String ROUTING_KEY = "notificacion.enviar";
    public static final String QUEUE = "notificaciones.queue";

    @Bean // ejecuta el metodo y lo registra como un bean en el contexto de Spring que guarda el objeto que devuelve
    public TopicExchange notificacionesExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue notificacionesQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    @Bean
    public Binding notificacionesBinding(Queue notificacionesQueue, TopicExchange notificacionesExchange) {
        return BindingBuilder.bind(notificacionesQueue).to(notificacionesExchange).with(ROUTING_KEY);
    }

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