package ar.utn.donatrack.logistica.integracion;

import ar.utn.donatrack.logistica.eventos.EntregaEvento;
import ar.utn.donatrack.logistica.eventos.TipoEventoLogistica;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El evento que Logística le manda al broker: el broker lo reenvía tal cual a
 * Donaciones, así que los nombres de campo tienen que ser los de Donaciones.
 */
@DisplayName("BrokerEventosListener - cuerpo del evento que recibe el broker")
class BrokerEventosListenerTest {

    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    private final CompletableFuture<String> recibido = new CompletableFuture<>();
    private HttpServer broker;

    @BeforeEach
    void levantarBroker() throws Exception {
        broker = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        broker.createContext("/api/broker/eventos/donatrack", intercambio -> {
            recibido.complete(new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            intercambio.sendResponseHeaders(202, -1);
            intercambio.close();
        });
        broker.start();
    }

    @AfterEach
    void apagarBroker() {
        broker.stop(0);
    }

    @Test
    @DisplayName("Entrega confirmada: lleva las fotos de la recepción en el campo fotos (Entrega 6)")
    void entregaConfirmadaConFotos() throws Exception {
        String url = "http://127.0.0.1:" + broker.getAddress().getPort() + "/api/broker/eventos/donatrack";
        UUID donacion = UUID.randomUUID();
        EntregaEvento evento = EntregaEvento.builder()
                .tipo(TipoEventoLogistica.ENTREGA_CONFIRMADA)
                .entregaId(UUID.randomUUID())
                .idDonacion(donacion)
                .idCamion(UUID.randomUUID())
                .patenteCamion("AB123CD")
                .fechaHoraEntrega(LocalDateTime.of(2026, 11, 12, 10, 30))
                .rutaId(UUID.randomUUID())
                .fotosComprobante(List.of("https://fotos.example/recibido.jpg"))
                .build();

        new BrokerEventosListener(url, json).onEvento(evento);

        JsonNode cuerpo = json.readTree(recibido.get(10, TimeUnit.SECONDS));
        assertThat(cuerpo.get("tipo").asText()).isEqualTo("ENTREGA_CONFIRMADA");
        assertThat(cuerpo.get("idDonacion").asText()).isEqualTo(donacion.toString());
        assertThat(cuerpo.get("patenteCamion").asText()).isEqualTo("AB123CD");
        assertThat(cuerpo.get("fotos")).hasSize(1);
        assertThat(cuerpo.get("fotos").get(0).asText()).isEqualTo("https://fotos.example/recibido.jpg");
    }
}
