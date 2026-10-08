package ar.utn.donatrack.incentivos.integracion.notificaciones;

import ar.utn.donatrack.incentivos.config.RabbitMQConfig;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * Tests del transporte asincrónico de incentivos hacia el Servicio de
 * Notificaciones.
 *
 * Incentivos es el segundo productor de la cola: cuando un donante completa una
 * misión o cambia de categoría, el aviso se publica en RabbitMQ en lugar de
 * viajar por HTTP. Es el mismo requisito de la Entrega 4 que ya cumple
 * donaciones, y los tres servicios comparten la misma topología.
 *
 * POR QUÉ IMPORTA QUE LOS NOMBRES COINCIDAN: si el exchange o la routing key de
 * este servicio se desalinean de los que declara notificaciones, el mensaje se
 * descarta y NADA FALLA A LA VISTA. Ya pasó una vez en este proyecto, con la
 * cola `notificaciones.pendientes` contra `notificaciones.queue`.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificacionTransportAmqp (incentivos) - publicación en la cola")
class NotificacionTransportAmqpTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private NotificacionTransportAmqp transporte;

    @Captor
    private ArgumentCaptor<SolicitudNotificacionMensaje> mensajeCaptor;

    private SolicitudNotificacionMensaje solicitud() {
        return new SolicitudNotificacionMensaje(
                "donante@example.com",
                "¡Completaste la misión Hábil Donador!",
                "EMAIL",
                "MISION_CUMPLIDA");
    }

    /** Publica y devuelve el mensaje que efectivamente salió. */
    private SolicitudNotificacionMensaje publicar(SolicitudNotificacionMensaje solicitud) {
        transporte.enviar(solicitud);
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), mensajeCaptor.capture());
        return mensajeCaptor.getValue();
    }

    @Nested
    @DisplayName("Destino del mensaje")
    class Destino {

        @Test
        @DisplayName("Publica en el exchange y la routing key de la topología compartida")
        void publicaEnElDestinoCorrecto() {
            transporte.enviar(solicitud());

            verify(rabbitTemplate).convertAndSend(
                    eq(RabbitMQConfig.EXCHANGE),
                    eq(RabbitMQConfig.ROUTING_KEY),
                    any(SolicitudNotificacionMensaje.class));
        }

        @Test
        @DisplayName("Usa exactamente los mismos nombres que donaciones y notificaciones")
        void nombresAlineadosEntreLosTresServicios() {
            // Están escritos a mano acá a propósito: si alguien cambia la
            // constante de un servicio y no la de los otros, este test falla.
            // Es la única defensa contra una desalineación silenciosa, porque
            // los tres módulos no comparten código.
            assertThat(RabbitMQConfig.EXCHANGE).isEqualTo("donatrack.notificaciones");
            assertThat(RabbitMQConfig.ROUTING_KEY).isEqualTo("notificacion.enviar");
            assertThat(RabbitMQConfig.QUEUE).isEqualTo("notificaciones.queue");
        }
    }

    @Nested
    @DisplayName("Contenido del payload")
    class Payload {

        @Test
        @DisplayName("Conserva los cuatro campos sin alterarlos")
        void payloadIntacto() {
            SolicitudNotificacionMensaje publicado = publicar(solicitud());

            assertThat(publicado.destinatario()).isEqualTo("donante@example.com");
            assertThat(publicado.mensaje()).isEqualTo("¡Completaste la misión Hábil Donador!");
            assertThat(publicado.medio()).isEqualTo("EMAIL");
            assertThat(publicado.evento()).isEqualTo("MISION_CUMPLIDA");
        }

        @Test
        @DisplayName("El campo evento viaja: es lo que distingue a este productor de donaciones")
        void incluyeElEvento() {
            // Donaciones manda 3 campos, incentivos manda 4. El consumidor los
            // recibe en el mismo record, con `evento` opcional. Si este campo
            // dejara de viajar, notificaciones perdería el motivo del aviso sin
            // que nada falle.
            SolicitudNotificacionMensaje publicado =
                    publicar(new SolicitudNotificacionMensaje(
                            "otro@example.com", "Subiste a Sostenedor", "EMAIL", "CAMBIO_CATEGORIA"));

            assertThat(publicado.evento()).isEqualTo("CAMBIO_CATEGORIA");
        }

        @Test
        @DisplayName("Respeta el medio elegido (EMAIL, SMS o WHATSAPP)")
        void respetaElMedio() {
            SolicitudNotificacionMensaje publicado =
                    publicar(new SolicitudNotificacionMensaje(
                            "1155667788", "Ganaste una insignia", "WHATSAPP", "INSIGNIA_OBTENIDA"));

            assertThat(publicado.medio()).isEqualTo("WHATSAPP");
        }
    }

    @Nested
    @DisplayName("Resiliencia ante fallas del broker")
    class Resiliencia {

        @Test
        @DisplayName("Si RabbitMQ está caído, NO propaga la excepción")
        void brokerCaidoNoRompeElFlujo() {
            // Una misión que el donante ya completó no puede deshacerse porque
            // el aviso posterior no se pudo encolar. Mismo criterio que el
            // transporte HTTP que este reemplaza.
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
