package ar.utn.donatrack.notificaciones.controller;

import ar.utn.donatrack.notificaciones.interfaces.repositories.NotificacionRepositoryInterface;
import ar.utn.donatrack.notificaciones.model.EstadoNotificacion;
import ar.utn.donatrack.notificaciones.model.Notificacion;
import ar.utn.donatrack.notificaciones.model.medios.Email;
import ar.utn.donatrack.notificaciones.model.medios.WhatsApp;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Quién ve qué notificaciones (Entrega 6): cada persona las suyas, el front con
 * su clave las de los demás medios de la persona, la administración todas; el
 * listado paginado desde 1 y el contador de no leídas.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:notificaciones_http;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "notificaciones.seguridad.clave-frontend=clave-del-frontend-solo-para-las-pruebas",
        "notificaciones.seguridad.clave-servicios=clave-de-servicios-solo-para-las-pruebas"
})
@AutoConfigureMockMvc
@DisplayName("Notificaciones: acceso, filtro por destinatario y paginación")
class NotificacionHttpTest {
    private static final String CLAVE_FRONT = "clave-del-frontend-solo-para-las-pruebas";
    private static HttpServer servidorClaves;
    private static RSAKey clave;

    @Autowired MockMvc http;
    @Autowired NotificacionRepositoryInterface repositorio;

    private String ana;
    private String beto;
    private String telefonoDeAna;
    private Notificacion deBeto;

    @DynamicPropertySource
    static void configurarClaves(DynamicPropertyRegistry propiedades) throws Exception {
        clave = new RSAKeyGenerator(2048).keyID("prueba-notificaciones").generate();
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

    @BeforeEach
    void sembrar() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        ana = "ana-" + sufijo + "@example.test";
        beto = "beto-" + sufijo + "@example.test";
        telefonoDeAna = "+54 11 " + sufijo;
        guardar(ana, true, 3);
        guardar(ana, false, 2);
        guardar(telefonoDeAna, false, 1);
        deBeto = guardar(beto, false, 0);
    }

    @Test
    @DisplayName("Sin token no se lee nada")
    void sinToken() throws Exception {
        http.perform(get("/notificaciones")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Se requiere un token."));
    }

    @Test
    @DisplayName("Cada persona ve las que le llegaron a su email, de la más nueva a la más vieja, paginadas desde 1")
    void lasPropias() throws Exception {
        http.perform(get("/notificaciones").header(HttpHeaders.AUTHORIZATION, token("DONANTE", ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(2))
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.contenido[0].leida").value(false));
        http.perform(get("/notificaciones").param("page", "2").param("size", "1")
                        .header(HttpHeaders.AUTHORIZATION, token("DONANTE", ana)))
                .andExpect(jsonPath("$.contenido.length()").value(1))
                .andExpect(jsonPath("$.totalPaginas").value(2))
                .andExpect(jsonPath("$.contenido[0].leida").value(true));
        http.perform(get("/notificaciones").param("page", "0").header(HttpHeaders.AUTHORIZATION, token("DONANTE", ana)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Las de sus otros medios (su WhatsApp) las pide el front con su clave; sin la clave, no")
    void otrosMediosSoloDesdeElFront() throws Exception {
        http.perform(get("/notificaciones").param("destinatario", ana, telefonoDeAna)
                        .header(HttpHeaders.AUTHORIZATION, token("DONANTE", ana)))
                .andExpect(status().isForbidden());
        http.perform(get("/notificaciones").param("destinatario", ana, telefonoDeAna)
                        .header(HttpHeaders.AUTHORIZATION, token("DONANTE", ana))
                        .header("X-DonaTrack-Frontend", CLAVE_FRONT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(3));
        http.perform(get("/notificaciones").param("destinatario", beto)
                        .header(HttpHeaders.AUTHORIZATION, token("DONANTE", ana)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("La administración ve todas o las de quien pida")
    void administracion() throws Exception {
        String admin = token("ADMINISTRADOR", "admin@example.test");
        http.perform(get("/notificaciones").param("destinatario", beto).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(1));
        http.perform(get("/notificaciones/{id}", deBeto.getId()).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk());
        http.perform(get("/notificaciones/{id}", deBeto.getId()).header(HttpHeaders.AUTHORIZATION, token("DONANTE", ana)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("El contador de no leídas baja a cero al marcarlas, sin tocar las de otra persona")
    void marcarLeidas() throws Exception {
        String deAna = token("DONANTE", ana);
        http.perform(get("/notificaciones/no-leidas").header(HttpHeaders.AUTHORIZATION, deAna))
                .andExpect(jsonPath("$.cantidad").value(1));
        http.perform(get("/notificaciones/no-leidas").param("destinatario", ana, telefonoDeAna)
                        .header(HttpHeaders.AUTHORIZATION, deAna).header("X-DonaTrack-Frontend", CLAVE_FRONT))
                .andExpect(jsonPath("$.cantidad").value(2));

        http.perform(patch("/notificaciones/leidas").header(HttpHeaders.AUTHORIZATION, deAna)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"destinatarios\":[\"" + beto + "\"]}"))
                .andExpect(status().isForbidden());
        http.perform(patch("/notificaciones/leidas").header(HttpHeaders.AUTHORIZATION, deAna)
                        .header("X-DonaTrack-Frontend", CLAVE_FRONT).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinatarios\":[\"" + ana + "\",\"" + telefonoDeAna + "\"]}"))
                .andExpect(status().isOk());

        http.perform(get("/notificaciones/no-leidas").param("destinatario", ana, telefonoDeAna)
                        .header(HttpHeaders.AUTHORIZATION, deAna).header("X-DonaTrack-Frontend", CLAVE_FRONT))
                .andExpect(jsonPath("$.cantidad").value(0));
        http.perform(get("/notificaciones/no-leidas").header(HttpHeaders.AUTHORIZATION, token("DONANTE", beto)))
                .andExpect(jsonPath("$.cantidad").value(1));
    }

    @Test
    @DisplayName("Enviar por HTTP es solo de los servicios de DonaTrack, con su clave")
    void enviarPideLaClaveDeServicios() throws Exception {
        http.perform(post("/notificaciones").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinatario\":\"x@example.test\",\"mensaje\":\"Hola\",\"medio\":\"EMAIL\"}"))
                .andExpect(status().isForbidden());
    }

    private Notificacion guardar(String destinatario, boolean leida, int horasAtras) {
        Notificacion notificacion = Notificacion.builder()
                .id(UUID.randomUUID())
                .destinatario(destinatario)
                .mensaje("Aviso de prueba")
                .medio(destinatario.startsWith("+") ? new WhatsApp() : new Email())
                .estado(EstadoNotificacion.ENVIADA)
                .fechaCreacion(LocalDateTime.now().minusHours(horasAtras))
                .build();
        if (leida) {
            notificacion.marcarLeida();
        }
        repositorio.guardar(notificacion);
        return notificacion;
    }

    private static String token(String rol, String email) throws Exception {
        var datos = new JWTClaimsSet.Builder()
                .issuer("urn:donatrack:autenticacion")
                .audience("donatrack-api")
                .subject(UUID.randomUUID().toString())
                .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                .claim("rol", rol)
                .claim("email", email)
                .build();
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(clave.getKeyID()).build(), datos);
        jwt.sign(new RSASSASigner(clave));
        return "Bearer " + jwt.serialize();
    }
}
