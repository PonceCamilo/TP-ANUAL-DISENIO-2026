package ar.utn.donatrack.donaciones.integracion.autenticacion;

import ar.utn.donatrack.donaciones.config.DonacionesProperties;
import ar.utn.donatrack.donaciones.config.PropiedadesDePrueba;
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

@DisplayName("EnviadorAutenticacion: contrato HTTP propuesto con el Servicio de Autenticación")
class EnviadorAutenticacionTest {

    private HttpServer servidor;
    private final Map<String, String> recibido = new ConcurrentHashMap<>();

    @BeforeEach
    void levantarServidor() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/api/autenticacion/cuentas", intercambio -> {
            recibido.put("ruta", intercambio.getRequestURI().getPath());
            recibido.put("cuerpo", new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String clave = intercambio.getRequestHeaders().getFirst(EnviadorAutenticacion.ENCABEZADO_CLAVE);
            recibido.put("clave", clave == null ? "(sin clave)" : clave);
            intercambio.sendResponseHeaders(204, -1);
            intercambio.close();
        });
        servidor.start();
    }

    @AfterEach
    void apagar() {
        servidor.stop(0);
    }

    private EnviadorAutenticacion enviadorCon(String clave) {
        DonacionesProperties base = PropiedadesDePrueba.porDefecto();
        DonacionesProperties propiedades = new DonacionesProperties(base.inactividad(), base.procesos(),
                base.importacion(), base.notificaciones(), base.matchmaking(), base.salida(), base.seguridad(),
                new DonacionesProperties.Cuentas(true, "http://localhost:" + servidor.getAddress().getPort(), clave, 50));
        return new EnviadorAutenticacion(RestClient.builder(), propiedades);
    }

    @Test
    @DisplayName("Manda el JSON guardado a /api/autenticacion/cuentas/importadas, con la clave si está configurada")
    void enviaElJsonGuardado() {
        String aviso = "{\"donantes\":[{\"donanteId\":\"d1\",\"email\":\"ana@mail.com\"}]}";

        enviadorCon("clave-de-servicio").enviar(Destinos.AUTENTICACION_CUENTAS_IMPORTADAS, aviso);

        assertThat(recibido.get("ruta")).isEqualTo("/api/autenticacion/cuentas/importadas");
        assertThat(recibido.get("cuerpo")).isEqualTo(aviso);
        assertThat(recibido.get("clave")).isEqualTo("clave-de-servicio");
    }

    @Test
    @DisplayName("Sin clave configurada no manda el encabezado")
    void sinClave() {
        EnviadorAutenticacion enviador = enviadorCon("");

        enviador.enviar(Destinos.AUTENTICACION_CUENTAS_IMPORTADAS, "{}");

        assertThat(recibido.get("clave")).isEqualTo("(sin clave)");
        assertThat(enviador.destinos()).containsExactly(Destinos.AUTENTICACION_CUENTAS_IMPORTADAS);
    }

    @Test
    @DisplayName("Si Autenticación no responde, la excepción sale para que el relay reintente")
    void autenticacionCaida() {
        EnviadorAutenticacion enviador = enviadorCon("");
        servidor.stop(0);

        assertThatThrownBy(() -> enviador.enviar(Destinos.AUTENTICACION_CUENTAS_IMPORTADAS, "{}"))
                .isInstanceOf(RuntimeException.class);
    }
}
