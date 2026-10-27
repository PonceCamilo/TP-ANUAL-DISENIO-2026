package ar.utn.donatrack.donaciones.clientes;

import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienGenerico;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("IncentivosClient: contrato con el servicio de incentivos")
class IncentivosClientTest {

    private HttpServer servidor;
    private final Map<String, String> recibidos = new ConcurrentHashMap<>();
    private IncentivosClient cliente;

    @BeforeEach
    void levantarServidor() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/api/incentivos/donantes", intercambio -> {
            recibidos.put(intercambio.getRequestURI().getPath(),
                    new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            intercambio.sendResponseHeaders(204, -1);
            intercambio.close();
        });
        servidor.start();
        cliente = new IncentivosClient("http://localhost:" + servidor.getAddress().getPort());
    }

    /** La fecha como la leería incentivos, sin importar si viaja como texto ISO o como arreglo. */
    private LocalDateTime fechaDe(String json) {
        try {
            ObjectMapper lector = new ObjectMapper().findAndRegisterModules();
            return lector.treeToValue(lector.readTree(json).get("fecha"), LocalDateTime.class);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @AfterEach
    void apagar() {
        servidor.stop(0);
    }

    @Test
    @DisplayName("La donación exitosa se informa con la fecha de entrega, no con la de registro")
    void donacionExitosaConFechaDeEntrega() {
        Donacion donacion = new Donacion();
        donacion.setIdDonante(UUID.randomUUID());
        donacion.setFechaDonacion(LocalDateTime.of(2026, 1, 10, 9, 0));
        donacion.getBienes().add(BienGenerico.builder().subcategoria(new Subcategoria("arroz"))
                .descripcion("arroz").cantidad(3).unidad("kg").build());

        cliente.notificarDonacionExitosa(donacion, "ana@mail.com", "WHATSAPP", LocalDateTime.of(2026, 3, 15, 14, 30));

        String cuerpo = recibidos.get("/api/incentivos/donantes/donacion-exitosa");
        assertThat(fechaDe(cuerpo)).isEqualTo(LocalDateTime.of(2026, 3, 15, 14, 30));
        assertThat(cuerpo)
                .contains("\"destinatario\":\"ana@mail.com\"")
                .contains("\"medio\":\"WHATSAPP\"")
                .contains("\"cantidadBienes\":3")
                .contains("\"categoriasDonadas\":[\"arroz\"]");
    }

    @Test
    @DisplayName("El registro de una donación usa los mismos campos que espera incentivos")
    void donacionRegistrada() {
        UUID donante = UUID.randomUUID();

        cliente.notificarDonacionRegistrada(donante, "ana@mail.com", "EMAIL", 2, List.of("arroz", "ropa"),
                LocalDateTime.of(2026, 3, 1, 10, 0));

        String cuerpo = recibidos.get("/api/incentivos/donantes/donacion");
        assertThat(fechaDe(cuerpo)).isEqualTo(LocalDateTime.of(2026, 3, 1, 10, 0));
        assertThat(cuerpo)
                .contains("\"donanteId\":\"" + donante + "\"")
                .contains("\"entidadBeneficiaria\":null");
    }

    @Test
    @DisplayName("Si incentivos no responde, el flujo de donaciones no se corta")
    void incentivosCaido() {
        servidor.stop(0);

        assertThatCode(() -> cliente.notificarDonacionRegistrada(UUID.randomUUID(), "a@b.com", "EMAIL", 1,
                List.of(), LocalDateTime.now())).doesNotThrowAnyException();
    }
}
