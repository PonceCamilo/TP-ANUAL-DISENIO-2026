package ar.utn.donatrack.donaciones.integracion.incentivos;

import ar.utn.donatrack.donaciones.salida.Destinos;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EnviadorIncentivos: contrato HTTP con el servicio de incentivos")
class EnviadorIncentivosTest {

    private HttpServer servidor;
    private final Map<String, String> recibidos = new ConcurrentHashMap<>();
    private final Map<String, String> tipos = new ConcurrentHashMap<>();
    private final Map<String, String> claves = new ConcurrentHashMap<>();
    private EnviadorIncentivos enviador;

    @BeforeEach
    void levantarServidor() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/api/incentivos/donantes", intercambio -> {
            String ruta = intercambio.getRequestURI().getPath();
            recibidos.put(ruta, new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            tipos.put(ruta, intercambio.getRequestHeaders().getFirst("Content-Type"));
            String clave = intercambio.getRequestHeaders().getFirst("X-DonaTrack-Servicio");
            claves.put(ruta, clave == null ? "(sin clave)" : clave);
            intercambio.sendResponseHeaders(204, -1);
            intercambio.close();
        });
        servidor.start();
        enviador = new EnviadorIncentivos(RestClient.builder(), "http://localhost:" + servidor.getAddress().getPort());
    }

    @AfterEach
    void apagar() {
        servidor.stop(0);
    }

    @Test
    @DisplayName("Cada destino va a su endpoint con el JSON tal como quedó en la bandeja")
    void enviaElJsonGuardado() {
        // El JSON lo armó el ObjectMapper de Spring: la fecha viaja en ISO-8601.
        String registrada = "{\"donanteId\":\"d1\",\"fecha\":\"2026-03-01T10:00:00\"}";
        String exitosa = "{\"donanteId\":\"d1\",\"fecha\":\"2026-03-15T14:30:00\"}";

        enviador.enviar(Destinos.INCENTIVOS_DONACION_REGISTRADA, registrada);
        enviador.enviar(Destinos.INCENTIVOS_DONACION_EXITOSA, exitosa);

        assertThat(recibidos.get("/api/incentivos/donantes/donacion")).isEqualTo(registrada);
        assertThat(recibidos.get("/api/incentivos/donantes/donacion-exitosa")).isEqualTo(exitosa);
        assertThat(tipos.get("/api/incentivos/donantes/donacion")).startsWith("application/json");
        assertThat(enviador.destinos()).containsExactlyInAnyOrder(
                Destinos.INCENTIVOS_DONACION_REGISTRADA, Destinos.INCENTIVOS_DONACION_EXITOSA);
    }

    @Test
    @DisplayName("Entrega 6: con la clave entre servicios configurada, la manda en cada aviso")
    void mandaLaClaveDeServicios() {
        EnviadorIncentivos conClave = new EnviadorIncentivos(RestClient.builder(),
                "http://localhost:" + servidor.getAddress().getPort(), "clave-de-servicios-de-al-menos-32-caracteres");

        conClave.enviar(Destinos.INCENTIVOS_DONACION_REGISTRADA, "{}");
        conClave.enviar(Destinos.INCENTIVOS_DONACION_EXITOSA, "{}");

        assertThat(claves.get("/api/incentivos/donantes/donacion")).isEqualTo("clave-de-servicios-de-al-menos-32-caracteres");
        assertThat(claves.get("/api/incentivos/donantes/donacion-exitosa")).isEqualTo("clave-de-servicios-de-al-menos-32-caracteres");
    }

    @Test
    @DisplayName("Sin clave configurada no manda la cabecera")
    void sinClave() {
        enviador.enviar(Destinos.INCENTIVOS_DONACION_REGISTRADA, "{}");
        assertThat(claves.get("/api/incentivos/donantes/donacion")).isEqualTo("(sin clave)");
    }

    @Test
    @DisplayName("Si incentivos no responde, la excepción sale para que el relay reintente")
    void incentivosCaido() {
        servidor.stop(0);

        assertThatThrownBy(() -> enviador.enviar(Destinos.INCENTIVOS_DONACION_REGISTRADA, "{}"))
                .isInstanceOf(RuntimeException.class);
    }
}
