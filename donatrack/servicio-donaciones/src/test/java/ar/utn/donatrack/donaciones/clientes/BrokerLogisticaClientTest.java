package ar.utn.donatrack.donaciones.clientes;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BrokerLogisticaClient: contrato HTTP con el broker de logística")
class BrokerLogisticaClientTest {

    private HttpServer servidor;
    private final AtomicReference<String> recibido = new AtomicReference<>();
    private final AtomicReference<String> respuesta = new AtomicReference<>();
    private final AtomicReference<Integer> codigo = new AtomicReference<>(200);
    private BrokerLogisticaClient cliente;

    @BeforeEach
    void levantarServidor() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/api/broker/envios", intercambio -> {
            recibido.set(new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] cuerpo = respuesta.get().getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(codigo.get(), cuerpo.length);
            intercambio.getResponseBody().write(cuerpo);
            intercambio.close();
        });
        servidor.start();
        cliente = new BrokerLogisticaClient(RestClient.builder(), "http://localhost:" + servidor.getAddress().getPort());
    }

    @AfterEach
    void apagar() {
        servidor.stop(0);
    }

    @Test
    @DisplayName("Manda el pedido al broker y lee qué proveedor tomó cada donación")
    void pideElEnvio() {
        UUID idDonacion = UUID.randomUUID();
        respuesta.set("""
                {"proveedor":"EXTERNA","proveedoresDescartados":["DONATRACK"],
                 "envios":[{"idDonacion":"%s","proveedor":"EXTERNA","idSeguimiento":"EXT-1",
                            "fechaAsignacion":"2026-03-15T14:30:00"}]}
                """.formatted(idDonacion));

        BrokerLogisticaClient.ResultadoEnvio resultado = cliente.solicitarEnvio(null, List.of(
                new BrokerLogisticaClient.DonacionEnvio(idDonacion, UUID.randomUUID(), "Comedor",
                        new BrokerLogisticaClient.DireccionEnvio("Medrano", 951, "CABA", "Buenos Aires", "1179"))));

        assertThat(recibido.get()).contains("\"idDonacion\":\"" + idDonacion + "\"").contains("\"calle\":\"Medrano\"");
        assertThat(resultado.proveedor()).isEqualTo("EXTERNA");
        assertThat(resultado.proveedoresDescartados()).containsExactly("DONATRACK");
        assertThat(resultado.envios()).singleElement().satisfies(envio -> {
            assertThat(envio.idSeguimiento()).isEqualTo("EXT-1");
            assertThat(envio.fechaAsignacion()).isEqualTo(LocalDateTime.of(2026, 3, 15, 14, 30));
        });
    }

    @Test
    @DisplayName("Si ningún proveedor está disponible (503), la excepción llega a quien despacha")
    void sinProveedores() {
        codigo.set(503);
        respuesta.set("{\"mensaje\":\"sin proveedores\"}");

        assertThatThrownBy(() -> cliente.solicitarEnvio("DONATRACK", List.of()))
                .isInstanceOf(RestClientResponseException.class)
                .satisfies(e -> assertThat(((RestClientResponseException) e).getStatusCode().value()).isEqualTo(503));
    }
}
