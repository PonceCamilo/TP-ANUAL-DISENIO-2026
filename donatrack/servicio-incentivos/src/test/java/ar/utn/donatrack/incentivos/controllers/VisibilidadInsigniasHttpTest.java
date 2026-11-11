package ar.utn.donatrack.incentivos.controllers;

import ar.utn.donatrack.incentivos.client.N8nWebhookClient;
import ar.utn.donatrack.incentivos.client.NotificacionClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class VisibilidadInsigniasHttpTest {
    private static final String DONANTES = "/api/incentivos/donantes";

    @Autowired MockMvc http;
    @Autowired ObjectMapper json;
    @MockitoBean NotificacionClient notificaciones;
    @MockitoBean N8nWebhookClient n8n;

    @Test
    void ocultarUnaInsigniaLaQuitaDelPerfilPublicoYPermiteMostrarlaDeNuevo() throws Exception {
        UUID donante = UUID.randomUUID();
        String actividad = """
                {
                  "donanteId": "%s",
                  "destinatario": "prueba@example.test",
                  "medio": "EMAIL",
                  "cantidadBienes": 1,
                  "categoriasDonadas": ["ALIMENTOS"],
                  "entidadBeneficiaria": "Comedor"
                }
                """.formatted(donante);

        http.perform(post(DONANTES + "/donacion").contentType(MediaType.APPLICATION_JSON).content(actividad))
                .andExpect(status().isCreated());
        http.perform(post(DONANTES + "/donacion-exitosa").contentType(MediaType.APPLICATION_JSON).content(actividad))
                .andExpect(status().isCreated());

        String perfil = DONANTES + "/{id}/perfil-publico";
        String respuesta = http.perform(get(perfil, donante))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoria").value("Colaborador"))
                .andExpect(jsonPath("$.insignias[0].nombre").value("Semilla de Solidaridad"))
                .andReturn().getResponse().getContentAsString();
        long insignia = json.readTree(respuesta).path("insignias").get(0).path("id").asLong();
        String visibilidad = DONANTES + "/{id}/insignias/{insignia}/visibilidad";

        http.perform(patch(visibilidad, UUID.randomUUID(), insignia)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"visible\":false}"))
                .andExpect(status().isNotFound());
        http.perform(patch(visibilidad, donante, insignia)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"visible\":false}"))
                .andExpect(status().isNoContent());
        http.perform(get(perfil, donante)).andExpect(jsonPath("$.insignias").isEmpty());
        http.perform(get(DONANTES + "/{id}/insignias", donante))
                .andExpect(jsonPath("$[0].visible").value(false));

        http.perform(patch(visibilidad, donante, insignia)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"visible\":true}"))
                .andExpect(status().isNoContent());
        http.perform(get(perfil, donante))
                .andExpect(jsonPath("$.insignias[0].id").value(insignia));
        http.perform(get("/api/incentivos/rankings/podio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.puestos[0].puesto").value(1));
    }
}
