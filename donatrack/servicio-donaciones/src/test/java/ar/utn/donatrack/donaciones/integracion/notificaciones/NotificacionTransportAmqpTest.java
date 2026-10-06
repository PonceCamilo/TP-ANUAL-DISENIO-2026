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
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
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
@DisplayName("NotificacionTransportAmqp - publicación en la cola de notificaciones")
class NotificacionTransportAmqpTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private NotificacionTransportAmqp transporte;

    @Captor
    private ArgumentCaptor<SolicitudNotificacionMensaje> mensajeCaptor;

    private SolicitudNotificacionMensaje solicitud() {
        return new SolicitudNotificacionMensaje(
                "juan@example.com", "Tu donación fue asignada a Comedor Los Pibes.", "EMAIL");
    }

    @Nested
    @DisplayName("Publicación del mensaje")
    class Publicacion {

        @Test
        @DisplayName("Publica en el exchange y la routing key configurados")
        void publicaEnElDestinoCorrecto() {
            // Si el exchange o la routing key no coinciden con los que declara
            // RabbitMQConfig, el mensaje se descarta en silencio.
            transporte.enviar(solicitud());

            verify(rabbitTemplate).convertAndSend(
                    org.mockito.ArgumentMatchers.eq(RabbitMQConfig.EXCHANGE),
                    org.mockito.ArgumentMatchers.eq(RabbitMQConfig.ROUTING_KEY),
                    any(SolicitudNotificacionMensaje.class));
        }

        @Test
        @DisplayName("El payload conserva destinatario, mensaje y medio sin alterarlos")
        void payloadIntacto() {
            // Los tres campos tienen que llegar tal cual: son exactamente los que
            // el consumidor espera en su SolicitudNotificacionDto.
            transporte.enviar(solicitud());

            verify(rabbitTemplate).convertAndSend(anyString(), anyString(), mensajeCaptor.capture());
            SolicitudNotificacionMensaje publicado = mensajeCaptor.getValue();

            assertThat(publicado.destinatario()).isEqualTo("juan@example.com");
            assertThat(publicado.mensaje()).isEqualTo("Tu donación fue asignada a Comedor Los Pibes.");
            assertThat(publicado.medio()).isEqualTo("EMAIL");
        }

        @Test
        @DisplayName("Respeta el medio elegido por el donante (EMAIL, SMS o WHATSAPP)")
        void respetaElMedio() {
            transporte.enviar(new SolicitudNotificacionMensaje("1155667788", "Hace rato que no donás", "WHATSAPP"));

            verify(rabbitTemplate).convertAndSend(anyString(), anyString(), mensajeCaptor.capture());
            assertThat(mensajeCaptor.getValue().medio()).isEqualTo("WHATSAPP");
        }
    }

    @Nested
    @DisplayName("Resiliencia ante fallas del broker")
    class Resiliencia {

        @Test
        @DisplayName("Si RabbitMQ está caído, NO propaga la excepción al flujo de negocio")
        void brokerCaidoNoRompeElFlujo() {
            // Mismo criterio que ya tenía el transporte HTTP: una donación que ya
            // se registró no puede reportarse como fallida porque el aviso
            // posterior no se pudo encolar.
            doThrow(new AmqpException("Connection refused"))
                    .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

            assertThatCode(() -> transporte.enviar(solicitud())).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Una excepción inesperada tampoco escapa del transporte")
        void cualquierErrorQuedaContenido() {
            doThrow(new RuntimeException("fallo inesperado de serialización"))
                    .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

            assertThatCode(() -> transporte.enviar(solicitud())).doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("Se identifica como transporte asincrónico en los logs de arranque")
    void nombreDelTransporte() {
        assertThat(transporte.nombre()).contains("asincrónico");
    }
}
