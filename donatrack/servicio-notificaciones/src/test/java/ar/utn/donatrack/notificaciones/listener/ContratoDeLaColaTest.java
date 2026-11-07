package ar.utn.donatrack.notificaciones.listener;

import ar.utn.donatrack.notificaciones.config.RabbitMQConfig;
import ar.utn.donatrack.notificaciones.dto.SolicitudNotificacionDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests del contrato de la cola, del lado del consumidor.
 *
 * POR QUÉ EXISTEN: los tres servicios que hablan por RabbitMQ no comparten una
 * sola línea de código. Donaciones publica su SolicitudNotificacionMensaje,
 * incentivos publica el suyo (con un campo más) y acá se recibe un
 * SolicitudNotificacionDto propio. Lo único que los une es el JSON que viaja, y
 * ningún compilador verifica que esos tres formatos calcen.
 *
 * Lo que ya se rompió en este proyecto por este motivo:
 *   - La cola se llamaba `notificaciones.pendientes` en un lado y
 *     `notificaciones.queue` en el otro: los mensajes se acumulaban en una cola
 *     que nadie leía y nada fallaba a la vista.
 *   - Faltaba el bean Jackson2JsonMessageConverter: sin él Spring AMQP usa
 *     serialización binaria de Java y no puede leer un mensaje de otro servicio.
 *
 * DETALLE IMPORTANTE: Jackson2JsonMessageConverter construye su PROPIO
 * ObjectMapper, no el que configura Spring Boot. Por eso los tests de acá usan
 * el converter de verdad y no un ObjectMapper armado a mano: lo que valga para
 * uno no necesariamente vale para el otro.
 */
@DisplayName("Contrato de la cola de notificaciones")
class ContratoDeLaColaTest {

    private final Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();

    /** Arma el mensaje tal como llega del broker, pidiendo el DTO de este servicio. */
    private Message mensajeConJson(String json) {
        MessageProperties propiedades = new MessageProperties();
        propiedades.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        propiedades.setContentEncoding(StandardCharsets.UTF_8.name());
        propiedades.getHeaders().put("__TypeId__", SolicitudNotificacionDto.class.getName());
        return new Message(json.getBytes(StandardCharsets.UTF_8), propiedades);
    }

    private SolicitudNotificacionDto deserializar(String json) {
        return (SolicitudNotificacionDto) converter.fromMessage(mensajeConJson(json));
    }

    @Nested
    @DisplayName("Payloads de los dos productores")
    class Payloads {

        @Test
        @DisplayName("El payload de donaciones (3 campos) se deserializa, con evento en null")
        void payloadDeDonaciones() {
            // Donaciones no manda `evento`. El record lo tiene como campo
            // opcional justamente para que pueda seguir sin mandarlo.
            SolicitudNotificacionDto dto = deserializar("""
                    {
                      "destinatario": "juan@example.com",
                      "mensaje": "Tu donación fue asignada a Comedor Los Andes.",
                      "medio": "EMAIL"
                    }
                    """);

            assertThat(dto.destinatario()).isEqualTo("juan@example.com");
            assertThat(dto.mensaje()).isEqualTo("Tu donación fue asignada a Comedor Los Andes.");
            assertThat(dto.medio()).isEqualTo("EMAIL");
            assertThat(dto.evento()).as("donaciones no manda este campo").isNull();
        }

        @Test
        @DisplayName("El payload de incentivos (4 campos) se deserializa con su evento")
        void payloadDeIncentivos() {
            // Incentivos agrega `evento` para decir qué disparó el aviso. El DTO
            // de acá lo contempla, así que el dato llega en vez de descartarse
            // —que es lo que pasaba antes de que se agregara el campo.
            SolicitudNotificacionDto dto = deserializar("""
                    {
                      "destinatario": "donante@example.com",
                      "mensaje": "¡Completaste la misión Hábil Donador!",
                      "medio": "EMAIL",
                      "evento": "MISION_CUMPLIDA"
                    }
                    """);

            assertThat(dto.evento()).isEqualTo("MISION_CUMPLIDA");
            assertThat(dto.mensaje()).contains("Hábil Donador");
        }

        @Test
        @DisplayName("Los acentos y signos sobreviven al viaje por la cola")
        void acentosIntactos() {
            // El mensaje viaja como bytes UTF-8. Si el encoding se perdiera, en
            // la base quedarían los caracteres partidos — y los mensajes de este
            // sistema están llenos de acentos y signos de apertura.
            SolicitudNotificacionDto dto = deserializar("""
                    {
                      "destinatario": "maría@example.com",
                      "mensaje": "¿Hace rato que no donás? Tu última donación fue en años anteriores.",
                      "medio": "WHATSAPP"
                    }
                    """);

            assertThat(dto.destinatario()).isEqualTo("maría@example.com");
            assertThat(dto.mensaje()).startsWith("¿Hace rato que no donás?");
            assertThat(dto.mensaje()).contains("última");
        }

        @Test
        @DisplayName("Los cuatro medios viajan como texto y llegan tal cual")
        void losMediosViajanComoTexto() {
            // El medio viaja como String y no como objeto: los productores no
            // conocen la jerarquía MedioNotificacion de este servicio. La
            // factory lo traduce al notificador concreto del otro lado.
            for (String medio : new String[]{"EMAIL", "SMS", "WHATSAPP", "DISCORD"}) {
                SolicitudNotificacionDto dto = deserializar("""
                        {"destinatario": "x@example.com", "mensaje": "hola", "medio": "%s"}
                        """.formatted(medio));

                assertThat(dto.medio()).isEqualTo(medio);
            }
        }
    }

    @Nested
    @DisplayName("Fragilidad del contrato")
    class Fragilidad {

        @Test
        @DisplayName("Un campo que el consumidor todavía no conoce se ignora, no rompe")
        void campoDesconocidoSeIgnora() {
            // Esto permite que los productores evolucionen su payload sin
            // coordinar un deploy simultáneo: si donaciones agrega un campo
            // antes de que este servicio lo contemple, los mensajes siguen
            // procesándose y el campo nuevo simplemente se descarta.
            //
            // No es el comportamiento por defecto de Jackson: viene de que
            // Jackson2JsonMessageConverter arma su ObjectMapper con
            // JacksonUtils.enhancedObjectMapper(), que apaga
            // FAIL_ON_UNKNOWN_PROPERTIES. Si alguien reemplazara ese bean por un
            // converter con un ObjectMapper propio, se perdería esta tolerancia
            // y los mensajes empezarían a fallar: por eso queda fijado acá.
            SolicitudNotificacionDto dto = deserializar("""
                    {
                      "destinatario": "x@example.com",
                      "mensaje": "hola",
                      "medio": "EMAIL",
                      "campoQueTodaviaNoExiste": "valor"
                    }
                    """);

            assertThat(dto.destinatario()).isEqualTo("x@example.com");
            assertThat(dto.medio()).isEqualTo("EMAIL");
        }

        @Test
        @DisplayName("Un campo obligatorio que falta llega como null, y lo frena la validación")
        void campoFaltanteLlegaNull() {
            // La deserialización no exige los campos: el que corta es el
            // @NotBlank del DTO, más arriba. Conviene tenerlo claro porque un
            // mensaje incompleto NO falla en el converter.
            SolicitudNotificacionDto dto = deserializar("""
                    {"destinatario": "x@example.com", "mensaje": "hola"}
                    """);

            assertThat(dto.medio()).isNull();
        }

        @Test
        @DisplayName("Los nombres de la topología son los que usan los tres servicios")
        void nombresDeLaTopologia() {
            // Escritos a mano a propósito. Los tres módulos declaran estas
            // constantes por separado, y si una se cambia sin las otras los
            // mensajes se pierden sin error. Es la única defensa posible sin
            // código compartido.
            assertThat(RabbitMQConfig.QUEUE).isEqualTo("notificaciones.queue");
        }
    }
}
