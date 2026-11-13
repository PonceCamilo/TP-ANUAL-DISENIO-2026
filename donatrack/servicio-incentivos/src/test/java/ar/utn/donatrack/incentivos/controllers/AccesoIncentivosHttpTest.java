package ar.utn.donatrack.incentivos.controllers;

import ar.utn.donatrack.incentivos.client.N8nWebhookClient;
import ar.utn.donatrack.incentivos.client.NotificacionClient;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Quién puede hacer qué en Incentivos (Entrega 6), con tokens firmados como los de
 * Autenticación y la clave entre servicios que manda Donaciones.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:acceso_incentivos;MODE=PostgreSQL",
        "servicios.clave-servicios=clave-de-servicios-solo-para-las-pruebas"
})
@AutoConfigureMockMvc
@DisplayName("Incentivos: permisos por rol, por persona y clave entre servicios")
class AccesoIncentivosHttpTest {
    private static final String DONANTES = "/api/incentivos/donantes";
    private static final String CLAVE = "clave-de-servicios-solo-para-las-pruebas";
    private static HttpServer servidorClaves;
    private static RSAKey clave;

    @Autowired MockMvc http;
    @MockitoBean NotificacionClient notificaciones;
    @MockitoBean N8nWebhookClient n8n;

    @DynamicPropertySource
    static void configurarClaves(DynamicPropertyRegistry propiedades) throws Exception {
        clave = new RSAKeyGenerator(2048).keyID("prueba-acceso").generate();
        byte[] clavesPublicas = new JWKSet(clave.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
        servidorClaves = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidorClaves.createContext("/claves", intercambio -> {
            try (intercambio) {
                intercambio.getResponseHeaders().set("Content-Type", "application/json");
                intercambio.sendResponseHeaders(200, clavesPublicas.length);
                intercambio.getResponseBody().write(clavesPublicas);
            }
        });
        servidorClaves.start();
        propiedades.add("servicios.autenticacion.jwk-set-uri",
                () -> "http://127.0.0.1:" + servidorClaves.getAddress().getPort() + "/claves");
    }

    @AfterAll
    static void detenerServidorClaves() {
        if (servidorClaves != null) {
            servidorClaves.stop(0);
        }
    }

    @Test
    @DisplayName("Las métricas, misiones e insignias de una persona las ve ella o la administración")
    void datosDeUnaPersona() throws Exception {
        UUID ana = UUID.randomUUID();
        for (String dato : new String[]{"/metricas", "/misiones", "/insignias"}) {
            http.perform(get(DONANTES + "/{id}" + dato, ana)).andExpect(status().isUnauthorized());
            http.perform(get(DONANTES + "/{id}" + dato, ana).header(HttpHeaders.AUTHORIZATION, token("DONANTE", UUID.randomUUID())))
                    .andExpect(status().isForbidden());
            http.perform(get(DONANTES + "/{id}" + dato, ana).header(HttpHeaders.AUTHORIZATION, token("DONANTE", ana)))
                    .andExpect(status().isOk());
            http.perform(get(DONANTES + "/{id}" + dato, ana).header(HttpHeaders.AUTHORIZATION, token("ADMINISTRADOR", null)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Los avisos de donación solo los manda Donaciones, y su nombre visible llega al podio")
    void avisosDeDonaciones() throws Exception {
        UUID ana = UUID.randomUUID();
        String aviso = """
                {"donanteId":"%s","destinatario":"ana@example.test","medio":"EMAIL","cantidadBienes":1,
                 "categoriasDonadas":["ALIMENTOS"],"entidadBeneficiaria":"Comedor","nombreVisible":"Ana P."}
                """.formatted(ana);

        http.perform(post(DONANTES + "/donacion").contentType(MediaType.APPLICATION_JSON).content(aviso))
                .andExpect(status().isForbidden());
        http.perform(post(DONANTES + "/donacion").header("X-DonaTrack-Servicio", "otra-clave-de-al-menos-32-caracteres")
                        .contentType(MediaType.APPLICATION_JSON).content(aviso))
                .andExpect(status().isForbidden());
        http.perform(post(DONANTES + "/donacion").header("X-DonaTrack-Servicio", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON).content(aviso))
                .andExpect(status().isCreated());
        http.perform(post(DONANTES + "/donacion-exitosa").header("X-DonaTrack-Servicio", CLAVE)
                        .contentType(MediaType.APPLICATION_JSON).content(aviso))
                .andExpect(status().isCreated());

        // El podio es público y nombra a la persona sin pedirle su perfil a Donaciones.
        http.perform(get("/api/incentivos/rankings/podio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.puestos[?(@.donanteId == '%s')].nombreVisible".formatted(ana)).value("Ana P."));
    }

    @Test
    @DisplayName("Qué insignias se muestran lo decide solo la persona")
    void visibilidadDeInsignias() throws Exception {
        UUID ana = UUID.randomUUID();
        String ruta = DONANTES + "/{id}/insignias/{insignia}/visibilidad";
        String cuerpo = "{\"visible\":false}";
        http.perform(patch(ruta, ana, 1).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isUnauthorized());
        http.perform(patch(ruta, ana, 1).header(HttpHeaders.AUTHORIZATION, token("ADMINISTRADOR", null))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isForbidden());
        http.perform(patch(ruta, ana, 1).header(HttpHeaders.AUTHORIZATION, token("DONANTE", UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("El podio y el perfil público siguen sin login")
    void publicos() throws Exception {
        http.perform(get("/api/incentivos/rankings/podio")).andExpect(status().isOk());
        http.perform(get(DONANTES + "/{id}/perfil-publico", UUID.randomUUID())).andExpect(status().isOk());
    }

    private static String token(String rol, UUID perfilId) throws Exception {
        var datos = new JWTClaimsSet.Builder()
                .issuer("urn:donatrack:autenticacion")
                .audience("donatrack-api")
                .subject(UUID.randomUUID().toString())
                .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                .claim("rol", rol);
        if (perfilId != null) {
            datos.claim("perfilId", perfilId.toString());
        }
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(clave.getKeyID()).build(), datos.build());
        jwt.sign(new RSASSASigner(clave));
        return "Bearer " + jwt.serialize();
    }
}
