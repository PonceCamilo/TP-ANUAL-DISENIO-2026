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
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DisplayName("Cuentas de las personas que cargó la administración (requisito 20)")
class CuentasImportadasTest {
    static final String CLAVE_SERVICIOS = "clave-de-servicios-solo-para-las-pruebas";

    @Autowired MockMvc http;
    @Autowired ObjectMapper json;

    @Test
    @DisplayName("Sin la clave entre servicios, o con otra, se rechaza el aviso")
    void exigeLaClaveDeServicios() throws Exception {
        byte[] aviso = aviso(persona(UUID.randomUUID(), "sin-clave@example.test", "HUMANA"));
        http.perform(post("/api/autenticacion/cuentas/importadas").contentType(MediaType.APPLICATION_JSON).content(aviso))
                .andExpect(status().isForbidden());
        http.perform(post("/api/autenticacion/cuentas/importadas").header("X-DonaTrack-Servicio", "otra-clave-de-servicios-de-32-caracteres")
                        .contentType(MediaType.APPLICATION_JSON).content(aviso))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Crea cada cuenta vinculada a su perfil, sin contraseña, y reimportar no cambia nada")
    void creaLasCuentasUnaSolaVez() throws Exception {
        UUID ana = UUID.randomUUID();
        UUID empresa = UUID.randomUUID();
        byte[] aviso = aviso(persona(ana, " Ana.Importada@Example.test ", "HUMANA"),
                persona(empresa, "empresa-importada@example.test", "JURIDICA"));

        enviar(aviso).andExpect(status().isOk())
                .andExpect(jsonPath("$.creadas").value(2))
                .andExpect(jsonPath("$.existentes").value(0))
                .andExpect(jsonPath("$.rechazadas").isEmpty());
        enviar(aviso).andExpect(status().isOk())
                .andExpect(jsonPath("$.creadas").value(0))
                .andExpect(jsonPath("$.existentes").value(2));

        // Todavía no eligió contraseña: no puede entrar, ni con una contraseña cualquiera.
        http.perform(post("/api/autenticacion/ingreso").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("email", "ana.importada@example.test",
                                "password", "cualquier-contraseña", "rol", "DONANTE"))))
                .andExpect(status().isUnauthorized());
        // Y registrarse con ese email la manda a activar la cuenta.
        http.perform(post("/api/autenticacion/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("email", "ana.importada@example.test", "nombre", "Ana",
                                "password", "contraseña-nueva-2026", "rol", "DONANTE", "tipoDonante", "HUMANA"))))
                .andExpect(status().isConflict())
                .andExpect(status().reason(containsString("Activá tu cuenta")));
    }

    @Test
    @DisplayName("No le entrega el perfil a una cuenta que ya existía con ese email")
    void noVinculaCuentasAjenas() throws Exception {
        http.perform(post("/api/autenticacion/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("email", "se-registro-antes@example.test", "nombre", "Alguien",
                                "password", "contraseña-de-prueba", "rol", "DONANTE", "tipoDonante", "HUMANA"))))
                .andExpect(status().isCreated());
        UUID persona = UUID.randomUUID();

        enviar(aviso(persona(persona, "se-registro-antes@example.test", "HUMANA"),
                persona(persona, "mismo-perfil-otro-email@example.test", "HUMANA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creadas").value(1))
                .andExpect(jsonPath("$.rechazadas.length()").value(1))
                .andExpect(jsonPath("$.rechazadas[0].email").value("se-registro-antes@example.test"));

        // El mismo perfil no puede quedar en dos cuentas.
        enviar(aviso(persona(persona, "tercer-email@example.test", "HUMANA")))
                .andExpect(jsonPath("$.creadas").value(0))
                .andExpect(jsonPath("$.rechazadas[0].motivo", containsString("otro email")));
    }

    @Test
    @DisplayName("Un dato mal cargado rechaza solo a esa persona, no a la tanda")
    void rechazaPorPersona() throws Exception {
        enviar(aviso(persona(UUID.randomUUID(), "no-es-un-email", "HUMANA"),
                persona(UUID.randomUUID(), "tipo-raro@example.test", "OTRO"),
                persona(UUID.randomUUID(), "valida-en-tanda@example.test", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creadas").value(1))
                .andExpect(jsonPath("$.rechazadas.length()").value(2));
    }

    private ResultActions enviar(byte[] aviso) throws Exception {
        return http.perform(post("/api/autenticacion/cuentas/importadas").header("X-DonaTrack-Servicio", CLAVE_SERVICIOS)
                .contentType(MediaType.APPLICATION_JSON).content(aviso));
    }

    private static Map<String, Object> persona(UUID id, String email, String tipo) {
        var persona = new java.util.HashMap<String, Object>();
        persona.put("donanteId", id);
        persona.put("email", email);
        persona.put("nombre", "Persona importada");
        persona.put("tipoDonante", tipo);
        return persona;
    }

    @SafeVarargs
    private byte[] aviso(Map<String, Object>... personas) throws Exception {
        return json.writeValueAsBytes(Map.of("donantes", List.of(personas)));
    }
}
