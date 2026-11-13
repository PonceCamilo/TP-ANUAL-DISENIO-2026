package ar.utn.donatrack.autenticacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DisplayName("Autenticación de DonaTrack")
class AutenticacionFlujoTest {
    @Autowired MockMvc http;
    @Autowired ObjectMapper json;

    @Test
    @DisplayName("Un donante jurídico se registra y el frontend vincula su perfil")
    void registrarDonanteYVincularPerfil() throws Exception {
        var cuenta = Map.of("email", "empresa@example.test", "nombre", "Representante", "password",
                "donante-test-2026", "rol", "DONANTE", "tipoDonante", "JURIDICA");
        String token = token(http.perform(post("/api/autenticacion/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(cuenta)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.tipoDonante").value("JURIDICA")).andReturn());

        http.perform(get("/api/autenticacion/sesion").header("Authorization", "Bearer " + token + "x"))
                .andExpect(status().isUnauthorized());

        http.perform(get("/api/autenticacion/claves-publicas"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.keys[0].kty").value("RSA"))
                .andExpect(jsonPath("$.keys[0].d").doesNotExist());

        UUID perfilId = UUID.randomUUID();
        byte[] perfil = json.writeValueAsBytes(Map.of("perfilId", perfilId));
        http.perform(post("/api/autenticacion/perfil").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(perfil))
                .andExpect(status().isForbidden());
        http.perform(post("/api/autenticacion/perfil").header("Authorization", "Bearer " + token)
                        .header("X-DonaTrack-Frontend", "clave-solo-para-la-prueba-de-integracion")
                        .contentType(MediaType.APPLICATION_JSON).content(perfil))
                .andExpect(status().isOk()).andExpect(jsonPath("$.perfilId").value(perfilId.toString()));
        http.perform(get("/api/autenticacion/sesion").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rol").value("DONANTE"))
                .andExpect(jsonPath("$.perfilId").value(perfilId.toString()));
    }

    @Test
    @DisplayName("El administrador crea al chofer y le asigna un camión")
    void crearChoferYAsignarCamion() throws Exception {
        http.perform(post("/api/autenticacion/registro").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"otro@example.test","nombre":"Otro","password":"contraseña-de-prueba","rol":"ADMINISTRADOR"}
                """)).andExpect(status().isForbidden());

        String admin = ingresar("admin@example.test", "admin-test-2026", "ADMINISTRADOR");
        byte[] alta = json.writeValueAsBytes(Map.of("nombre", "Chofer de prueba",
                "email", "chofer@example.test", "password", "chofer-test-2026"));
        http.perform(post("/api/autenticacion/choferes").contentType(MediaType.APPLICATION_JSON).content(alta))
                .andExpect(status().isUnauthorized());
        http.perform(post("/api/autenticacion/choferes").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content(alta))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("CHOFER"))
                .andExpect(jsonPath("$.camionId").isEmpty());
        String chofer = ingresar("chofer@example.test", "chofer-test-2026", "CHOFER");
        http.perform(post("/api/autenticacion/choferes").header("Authorization", "Bearer " + chofer)
                        .contentType(MediaType.APPLICATION_JSON).content(alta))
                .andExpect(status().isForbidden());
        String perfil = http.perform(get("/api/autenticacion/sesion").header("Authorization", "Bearer " + chofer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.camionId").isEmpty())
                .andReturn().getResponse().getContentAsString();
        String id = json.readTree(perfil).get("id").asText();
        UUID camion = UUID.randomUUID();
        var asignacion = json.writeValueAsBytes(Map.of("camionId", camion));

        http.perform(put("/api/autenticacion/choferes/{id}/camion", id).header("Authorization", "Bearer " + chofer)
                        .contentType(MediaType.APPLICATION_JSON).content(asignacion))
                .andExpect(status().isForbidden());
        http.perform(put("/api/autenticacion/choferes/{id}/camion", id).header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content(asignacion))
                .andExpect(status().isOk());
        http.perform(get("/api/autenticacion/sesion").header("Authorization", "Bearer " + chofer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.camionId").value(camion.toString()));
    }

    private String ingresar(String email, String password, String rol) throws Exception {
        return token(http.perform(post("/api/autenticacion/ingreso").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("email", email, "password", password, "rol", rol))))
                .andExpect(status().isOk()).andReturn());
    }

    private String token(MvcResult respuesta) throws Exception {
        return json.readTree(respuesta.getResponse().getContentAsByteArray()).get("accessToken").asText();
    }
}
