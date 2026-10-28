package ar.utn.donatrack.donaciones.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.service.annotation.GetExchange;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static ar.utn.donatrack.donaciones.config.IdDeCorrelacion.CLAVE_MDC;
import static ar.utn.donatrack.donaciones.config.IdDeCorrelacion.ENCABEZADO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("IdDeCorrelacion - un id por pedido, en los logs, la respuesta y las llamadas salientes")
class IdDeCorrelacionTest {

    private final IdDeCorrelacion filtro = new IdDeCorrelacion();

    @AfterEach
    void limpiarMdc() {
        MDC.clear();
    }

    @Nested
    @DisplayName("Pedidos que llegan")
    class PedidosQueLlegan {

        /** Pasa un pedido por el filtro y devuelve el id que vio el resto de la cadena. */
        private String atender(MockHttpServletRequest pedido, MockHttpServletResponse respuesta) throws Exception {
            AtomicReference<String> visto = new AtomicReference<>();
            filtro.doFilter(pedido, respuesta, (req, res) -> visto.set(MDC.get(CLAVE_MDC)));
            return visto.get();
        }

        @Test
        @DisplayName("Usa el id que trae el pedido, lo devuelve y lo saca del MDC al terminar")
        void usaElRecibido() throws Exception {
            MockHttpServletRequest pedido = new MockHttpServletRequest();
            pedido.addHeader(ENCABEZADO, "broker-7f3a.1");
            MockHttpServletResponse respuesta = new MockHttpServletResponse();

            assertThat(atender(pedido, respuesta)).isEqualTo("broker-7f3a.1");
            assertThat(respuesta.getHeader(ENCABEZADO)).isEqualTo("broker-7f3a.1");
            assertThat(MDC.get(CLAVE_MDC)).isNull();
        }

        @Test
        @DisplayName("Sin id, genera uno nuevo")
        void generaUnoNuevo() throws Exception {
            MockHttpServletResponse respuesta = new MockHttpServletResponse();

            String id = atender(new MockHttpServletRequest(), respuesta);

            assertThat(UUID.fromString(id)).isNotNull();
            assertThat(respuesta.getHeader(ENCABEZADO)).isEqualTo(id);
        }

        @Test
        @DisplayName("Un id con otra forma (saltos de línea, demasiado largo) se reemplaza: va a los logs")
        void reemplazaLosInvalidos() throws Exception {
            for (String invalido : new String[]{"abc\nINFO pedido falso", "x".repeat(65), "id con espacios"}) {
                MockHttpServletRequest pedido = new MockHttpServletRequest();
                pedido.addHeader(ENCABEZADO, invalido);

                String id = atender(pedido, new MockHttpServletResponse());

                assertThat(id).isNotEqualTo(invalido);
                assertThat(UUID.fromString(id)).isNotNull();
            }
        }
    }

    @Nested
    @DisplayName("Llamadas a otros servicios")
    class LlamadasSalientes {

        interface Ping {
            @GetExchange("/ping")
            String ping();
        }

        private final RestClient.Builder builder = RestClient.builder();
        private final MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        private final Ping otroServicio = ClientesHttp.crear(builder, "http://otro-servicio", Ping.class);

        @Test
        @DisplayName("Llevan el id del pedido en curso")
        void llevanElId() {
            servidor.expect(requestTo("http://otro-servicio/ping"))
                    .andExpect(header(ENCABEZADO, "pedido-42"))
                    .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));
            MDC.put(CLAVE_MDC, "pedido-42");

            assertThat(otroServicio.ping()).isEqualTo("ok");
            servidor.verify();
        }

        @Test
        @DisplayName("Fuera de un pedido (un proceso nocturno) salen sin id")
        void sinPedidoNoLlevanId() {
            servidor.expect(requestTo("http://otro-servicio/ping"))
                    .andExpect(headerDoesNotExist(ENCABEZADO))
                    .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));

            otroServicio.ping();
            servidor.verify();
        }
    }
}
