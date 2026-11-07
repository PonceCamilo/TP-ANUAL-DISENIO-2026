package ar.utn.donatrack.donaciones.integracion.notificaciones;

import ar.utn.donatrack.donaciones.config.RabbitMQConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitOperations;
import org.springframework.amqp.AmqpTimeoutException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * Tests del transporte asincrónico hacia el Servicio de Notificaciones.
 *
 * Es la implementación del requisito de integración de la Entrega 4: el aviso
 * se publica en una cola y el flujo de negocio sigue sin esperar a que el otro
 * servicio lo procese.
 *
 * Lo que se verifica acá es el contrato de publicación (exchange, routing key y
 * payload) y, sobre todo, que una falla del broker no se propague al flujo que
 * originó la notificación.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificacionTransportAmqp - publicación con confirmación del broker")
class NotificacionTransportAmqpTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private RabbitOperations operaciones;

    @InjectMocks
    private NotificacionTransportAmqp transporte;

    @Captor
    private ArgumentCaptor<SolicitudNotificacionMensaje> mensajeCaptor;

    private SolicitudNotificacionMensaje solicitud() {
        return new SolicitudNotificacionMensaje(
                "juan@example.com", "Tu donación fue asignada a Comedor Los Pibes.", "EMAIL");
    }

    /** El template ejecuta el bloque sobre un canal con confirmaciones (acá, simulado). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void conCanal() {
        when(rabbitTemplate.invoke(any(RabbitOperations.OperationsCallback.class))).thenAnswer(invocacion ->
                ((RabbitOperations.OperationsCallback) invocacion.getArgument(0)).doInRabbit(operaciones));
    }

    @Nested
    @DisplayName("Publicación del mensaje")
    class Publicacion {

        @Test
        @DisplayName("Publica en el exchange y la routing key configurados y espera la confirmación")
        void publicaYEsperaLaConfirmacion() {
            // Si el exchange o la routing key no coinciden con los que declara
            // RabbitMQConfig, el mensaje se descarta en silencio.
            conCanal();

            transporte.enviar(solicitud());

            verify(operaciones).convertAndSend(eq(RabbitMQConfig.EXCHANGE), eq(RabbitMQConfig.ROUTING_KEY),
                    any(SolicitudNotificacionMensaje.class));
            verify(operaciones).waitForConfirmsOrDie(NotificacionTransportAmqp.TIEMPO_CONFIRMACION_MS);
        }

        @Test
        @DisplayName("El payload conserva destinatario, mensaje y medio sin alterarlos")
        void payloadIntacto() {
            // Los tres campos tienen que llegar tal cual: son exactamente los que
            // el consumidor espera en su SolicitudNotificacionDto.
            conCanal();

            transporte.enviar(new SolicitudNotificacionMensaje("1155667788", "Hace rato que no donás", "WHATSAPP"));

            verify(operaciones).convertAndSend(anyString(), anyString(), mensajeCaptor.capture());
            assertThat(mensajeCaptor.getValue())
                    .isEqualTo(new SolicitudNotificacionMensaje("1155667788", "Hace rato que no donás", "WHATSAPP"));
        }
    }

    @Nested
    @DisplayName("Fallas del broker")
    class Fallas {

        @Test
        @DisplayName("Si RabbitMQ está caído, la excepción sale: el aviso sigue en la bandeja y se reintenta")
        void brokerCaido() {
            when(rabbitTemplate.invoke(any())).thenThrow(new AmqpException("Connection refused"));

            assertThatThrownBy(() -> transporte.enviar(solicitud())).isInstanceOf(AmqpException.class);
        }

        @Test
        @DisplayName("Si el broker no confirma a tiempo (o rechaza el mensaje), también falla")
        void sinConfirmacion() {
            conCanal();
            doThrow(new AmqpTimeoutException("sin confirmación"))
                    .when(operaciones).waitForConfirmsOrDie(anyLong());

            assertThatThrownBy(() -> transporte.enviar(solicitud())).isInstanceOf(AmqpTimeoutException.class);
        }
    }

    @Test
    @DisplayName("Se identifica como transporte asincrónico en los logs de arranque")
    void nombreDelTransporte() {
        assertThat(transporte.nombre()).contains("asincrónico");
    }
}
