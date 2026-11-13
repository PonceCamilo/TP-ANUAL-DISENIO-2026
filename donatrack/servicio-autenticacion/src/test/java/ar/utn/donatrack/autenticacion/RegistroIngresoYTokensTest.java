package ar.utn.donatrack.autenticacion;

import ar.utn.donatrack.autenticacion.seguridad.GoogleIdentidad;
import ar.utn.donatrack.autenticacion.seguridad.Tokens;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DisplayName("Registro, ingreso, Google y tokens")
class RegistroIngresoYTokensTest {
    @Autowired MockMvc http;
    @Autowired ObjectMapper json;
    @Autowired Tokens tokens;
    @MockitoBean GoogleIdentidad google;

    @Nested
    @DisplayName("Registro e ingreso con email")
    class ConEmail {
        @Test
        @DisplayName("El email repetido, la contraseña corta y los roles internos se rechazan")
        void validaciones() throws Exception {
            registrar("repetido@example.test", "contraseña-de-prueba", "DONANTE").andExpect(status().isCreated());
            registrar("REPETIDO@example.test", "otra-contraseña-larga", "DONANTE").andExpect(status().isConflict());
            registrar("corta@example.test", "corta", "DONANTE").andExpect(status().isBadRequest());
            registrar("chofer-solo@example.test", "contraseña-de-prueba", "CHOFER").andExpect(status().isForbidden());
            // El mismo email puede tener una cuenta por perfil.
            registrar("repetido@example.test", "contraseña-de-prueba", "ENTIDAD_BENEFICIARIA").andExpect(status().isCreated());
        }

        @Test
        @DisplayName("La contraseña equivocada, el perfil equivocado o la cuenta inexistente dan el mismo 401")
        void ingresoIncorrecto() throws Exception {
            registrar("ingreso@example.test", "contraseña-de-prueba", "DONANTE").andExpect(status().isCreated());
            ingresar("ingreso@example.test", "contraseña-de-prueba", "DONANTE").andExpect(status().isOk());
            ingresar("ingreso@example.test", "otra-contraseña", "DONANTE").andExpect(status().isUnauthorized())
                    .andExpect(status().reason("Email, contraseña o perfil incorrectos."));
            ingresar("ingreso@example.test", "contraseña-de-prueba", "ENTIDAD_BENEFICIARIA").andExpect(status().isUnauthorized())
                    .andExpect(status().reason("Email, contraseña o perfil incorrectos."));
            ingresar("no-existe@example.test", "contraseña-de-prueba", "DONANTE").andExpect(status().isUnauthorized())
                    .andExpect(status().reason("Email, contraseña o perfil incorrectos."));
        }

        @Test
        @DisplayName("El token dice quién es, con qué rol y perfil, y vence en una hora")
        void contenidoDelToken() throws Exception {
            String respuesta = registrar("claims@example.test", "contraseña-de-prueba", "DONANTE")
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.expiresIn").value(3600))
                    .andReturn().getResponse().getContentAsString();
            Jwt token = tokens.verificar(json.readTree(respuesta).get("accessToken").asText());

            assertThat(token.getClaimAsString("iss")).isEqualTo("urn:donatrack:autenticacion");
            assertThat(token.getAudience()).containsExactly("donatrack-api");
            assertThat(token.getClaimAsString("email")).isEqualTo("claims@example.test");
            assertThat(token.getClaimAsString("rol")).isEqualTo("DONANTE");
            assertThat(token.getClaimAsString("tipoDonante")).isEqualTo("HUMANA");
            assertThat(token.hasClaim("perfilId")).isFalse();
            assertThat(Duration.between(token.getIssuedAt(), token.getExpiresAt())).isEqualTo(Duration.ofHours(1));
            UUID.fromString(token.getSubject());
        }

        @Test
        @DisplayName("Los choferes solo los lista la administración")
        void listaDeChoferes() throws Exception {
            http.perform(get("/api/autenticacion/choferes")).andExpect(status().isUnauthorized());
            String donante = json.readTree(registrar("curioso@example.test", "contraseña-de-prueba", "DONANTE")
                    .andReturn().getResponse().getContentAsString()).get("accessToken").asText();
            http.perform(get("/api/autenticacion/choferes").header("Authorization", "Bearer " + donante))
                    .andExpect(status().isForbidden());
            String admin = json.readTree(ingresar("admin@example.test", "admin-test-2026", "ADMINISTRADOR")
                    .andReturn().getResponse().getContentAsString()).get("accessToken").asText();
            http.perform(get("/api/autenticacion/choferes").header("Authorization", "Bearer " + admin))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Ingreso con Google")
    class ConGoogle {
        @Test
        @DisplayName("El primer ingreso crea la cuenta y los siguientes entran a la misma")
        void primerYSegundoIngreso() throws Exception {
            when(google.verificar("token-google-ana")).thenReturn(identidad("sub-ana", "Ana.Google@Example.test", "Ana"));

            String primera = conGoogle("token-google-ana", "DONANTE").andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("ana.google@example.test"))
                    .andExpect(jsonPath("$.nombre").value("Ana"))
                    .andReturn().getResponse().getContentAsString();
            String segunda = conGoogle("token-google-ana", "DONANTE").andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            assertThat(tokens.verificar(json.readTree(segunda).get("accessToken").asText()).getSubject())
                    .isEqualTo(tokens.verificar(json.readTree(primera).get("accessToken").asText()).getSubject());
        }

        @Test
        @DisplayName("Si ya hay una cuenta con contraseña para ese email, pide entrar con el método original")
        void emailYaRegistrado() throws Exception {
            registrar("con-clave@example.test", "contraseña-de-prueba", "DONANTE").andExpect(status().isCreated());
            when(google.verificar("token-google-repetido")).thenReturn(identidad("sub-repetido", "con-clave@example.test", "Otra"));

            conGoogle("token-google-repetido", "DONANTE").andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Google no confirma la identidad: 401; perfil interno: 403; otro proveedor: 400")
        void rechazos() throws Exception {
            when(google.verificar("token-falso")).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google no confirmó tu identidad."));
            conGoogle("token-falso", "DONANTE").andExpect(status().isUnauthorized());
            conGoogle("token-falso", "ADMINISTRADOR").andExpect(status().isForbidden());
            http.perform(post("/api/autenticacion/ingreso-social").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"proveedor\":\"facebook\",\"idToken\":\"x\",\"rol\":\"DONANTE\"}"))
                    .andExpect(status().isBadRequest());
        }

        private ResultActions conGoogle(String idToken, String rol) throws Exception {
            return http.perform(post("/api/autenticacion/ingreso-social").contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsBytes(Map.of("proveedor", "google", "idToken", idToken, "rol", rol))));
        }

        private Jwt identidad(String sub, String email, String nombre) {
            return Jwt.withTokenValue("id-token").header("alg", "RS256").subject(sub)
                    .claim("email", email).claim("name", nombre).claim("email_verified", true)
                    .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).build();
        }
    }

    private ResultActions registrar(String email, String password, String rol) throws Exception {
        return http.perform(post("/api/autenticacion/registro").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(Map.of("email", email, "nombre", "Persona de prueba",
                        "password", password, "rol", rol))));
    }

    private ResultActions ingresar(String email, String password, String rol) throws Exception {
        return http.perform(post("/api/autenticacion/ingreso").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(Map.of("email", email, "password", password, "rol", rol))));
    }
}
