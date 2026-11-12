package ar.utn.donatrack.incentivos.controllers;

import ar.utn.donatrack.incentivos.interfaces.repositories.RankingMensualRepositoryInterface;
import ar.utn.donatrack.incentivos.models.RankingMensual;
import ar.utn.donatrack.incentivos.services.IncentivosService;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.YearMonth;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:ranking_admin;MODE=PostgreSQL")
@AutoConfigureMockMvc
class RankingAdminHttpTest {
    private static final String RUTA = "/api/incentivos/admin/rankings";
    private static final String MESES = RUTA + "/meses";
    private static HttpServer servidorClaves;
    private static RSAKey clave;

    @Autowired MockMvc http;
    @Autowired IncentivosService incentivos;
    @Autowired RankingMensualRepositoryInterface rankings;

    @DynamicPropertySource
    static void configurarClaves(DynamicPropertyRegistry propiedades) throws Exception {
        clave = new RSAKeyGenerator(2048).keyID("prueba-ranking").generate();
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
    void administradorConsultaCierresYPuestosPaginados() throws Exception {
        YearMonth actual = YearMonth.now();
        YearMonth cerrado = actual.minusMonths(1);
        var primero = incentivos.obtenerPerfil(UUID.randomUUID());
        var segundo = incentivos.obtenerPerfil(UUID.randomUUID());
        rankings.guardar(RankingMensual.calcular(List.of(primero, segundo), cerrado.atEndOfMonth().atTime(23, 59)));

        String admin = credencial("ADMINISTRADOR");
        http.perform(get(MESES)).andExpect(status().isUnauthorized());
        http.perform(get(MESES).header(HttpHeaders.AUTHORIZATION, credencial("DONANTE")))
                .andExpect(status().isForbidden());
        http.perform(get(MESES).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].anio").value(cerrado.getYear()))
                .andExpect(jsonPath("$[0].mes").value(cerrado.getMonthValue()));
        http.perform(get(ruta(cerrado)).header(HttpHeaders.AUTHORIZATION, admin)
                        .param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provisional").value(false))
                .andExpect(jsonPath("$.totalElementos").value(2))
                .andExpect(jsonPath("$.contenido[0].puesto").value(2));
        http.perform(get(ruta(actual)).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.provisional").value(true));
        http.perform(get(ruta(actual.minusMonths(2))).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNotFound());
        http.perform(get(RUTA + "/2026/13").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isBadRequest());
    }

    private static String ruta(YearMonth periodo) {
        return RUTA + "/" + periodo.getYear() + "/" + periodo.getMonthValue();
    }

    private static String credencial(String rol) throws Exception {
        var datos = new JWTClaimsSet.Builder()
                .issuer("urn:donatrack:autenticacion")
                .audience("donatrack-api")
                .subject(UUID.randomUUID().toString())
                .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                .claim("rol", rol)
                .build();
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(clave.getKeyID()).build(), datos);
        jwt.sign(new RSASSASigner(clave));
        return "Bearer " + jwt.serialize();
    }
}
