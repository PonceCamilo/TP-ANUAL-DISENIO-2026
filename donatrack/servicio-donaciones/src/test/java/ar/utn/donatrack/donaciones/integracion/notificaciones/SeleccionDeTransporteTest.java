package ar.utn.donatrack.donaciones.integracion.notificaciones;

import ar.utn.donatrack.donaciones.interfaces.integracion.NotificacionTransport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que la property `notificaciones.transporte` realmente elija la
 * implementación correcta al levantar el contexto.
 *
 * POR QUÉ IMPORTA: toda la estrategia de migración a la cola depende de este
 * interruptor. Mientras el consumidor del lado de servicio-notificaciones no
 * exista, donaciones tiene que seguir funcionando por REST; cuando esté, se
 * cambia una property y nada más. Si el cableado condicional se rompiera, el
 * síntoma sería silencioso: notificaciones que se publican a una cola que nadie
 * lee, o un arranque fallido por falta de RabbitMQ.
 *
 * Estos tests levantan el contexto completo, que es el único lugar donde se
 * puede comprobar un @ConditionalOnProperty.
 */
@DisplayName("Selección del transporte de notificaciones por configuración")
class SeleccionDeTransporteTest {

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
    @TestPropertySource(properties = "notificaciones.transporte=rest")
    @DisplayName("Con notificaciones.transporte=rest")
    class ModoRest {

        @Autowired
        private NotificacionTransport transporte;

        @Autowired
        private ApplicationContext contexto;

        @Test
        @DisplayName("Se inyecta el transporte HTTP sincrónico")
        void seUsaElTransporteRest() {
            assertThat(transporte).isInstanceOf(NotificacionTransportRest.class);
        }

        @Test
        @DisplayName("NO se crean los beans de RabbitMQ, así que no hace falta el broker levantado")
        void noSeCreaLaTopologiaDeRabbit() {
            // Es lo que permite que el servicio siga arrancando hoy, sin RabbitMQ
            // y sin que el equipo tenga que montar nada todavía.
            assertThat(contexto.containsBean("notificacionesQueue")).isFalse();
            assertThat(contexto.containsBean("notificacionesExchange")).isFalse();
        }
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
    @TestPropertySource(properties = "notificaciones.transporte=amqp")
    @DisplayName("Con notificaciones.transporte=amqp")
    class ModoAmqp {

        @Autowired
        private NotificacionTransport transporte;

        @Autowired
        private ApplicationContext contexto;

        @Test
        @DisplayName("Se inyecta el transporte asincrónico por cola")
        void seUsaElTransporteAmqp() {
            assertThat(transporte).isInstanceOf(NotificacionTransportAmqp.class);
        }

        @Test
        @DisplayName("Se declara la topología completa: exchange, cola y binding")
        void seDeclaraLaTopologia() {
            assertThat(contexto.containsBean("notificacionesExchange")).isTrue();
            assertThat(contexto.containsBean("notificacionesQueue")).isTrue();
            assertThat(contexto.containsBean("notificacionesBinding")).isTrue();
        }

        @Test
        @DisplayName("El contexto levanta aunque RabbitMQ no esté corriendo")
        void arrancaSinBrokerDisponible() {
            // La conexión de Spring AMQP es perezosa: recién se intenta al
            // publicar el primer mensaje. Por eso el servicio no queda atado a
            // que el broker esté arriba en el momento del arranque.
            assertThat(transporte).isNotNull();
        }
    }
}
