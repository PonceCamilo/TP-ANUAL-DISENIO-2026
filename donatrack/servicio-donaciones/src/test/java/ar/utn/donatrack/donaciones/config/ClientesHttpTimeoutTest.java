package ar.utn.donatrack.donaciones.config;

import ar.utn.donatrack.donaciones.clientes.BrokerLogisticaClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Los clientes hacia otros servicios usan el RestClient.Builder de Spring Boot y,
 * con él, el timeout de lectura configurado: un servicio que no contesta corta la
 * llamada en lugar de dejar el hilo colgado.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.http.client.read-timeout=300ms")
@DisplayName("Clientes HTTP: el timeout configurado se aplica")
class ClientesHttpTimeoutTest {

    /** Un broker que tarda más que el timeout en contestar. */
    private static final HttpServer LENTO = lento();

    private static HttpServer lento() {
        try {
            HttpServer servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            servidor.createContext("/", intercambio -> {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                intercambio.sendResponseHeaders(200, -1);
                intercambio.close();
            });
            servidor.start();
            return servidor;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @DynamicPropertySource
    static void urlDelBroker(DynamicPropertyRegistry registro) {
        registro.add("servicios.broker-logistica.url", () -> "http://localhost:" + LENTO.getAddress().getPort());
    }

    @AfterAll
    static void apagar() {
        LENTO.stop(0);
    }

    @Autowired
    private BrokerLogisticaClient broker;

    @Test
    @DisplayName("Un broker que no contesta corta por timeout, sin esperar su respuesta")
    void cortaPorTimeout() {
        long inicio = System.nanoTime();

        assertThatThrownBy(() -> broker.solicitarEnvio(null, List.of())).isInstanceOf(ResourceAccessException.class);

        assertThat(Duration.ofNanos(System.nanoTime() - inicio)).isLessThan(Duration.ofSeconds(2));
    }
}
