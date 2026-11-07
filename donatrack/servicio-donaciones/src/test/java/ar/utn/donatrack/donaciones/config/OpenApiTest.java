package ar.utn.donatrack.donaciones.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** La especificación OpenAPI que publica el servicio (/v3/api-docs). */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("OpenAPI: documentación completa y errores con su esquema")
class OpenApiTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;

    private JsonNode especificacion() throws Exception {
        return json.readTree(mvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString());
    }

    @Test
    @DisplayName("Las respuestas de error declaran el esquema de error común y toda operación documenta el 500")
    void erroresConEsquema() throws Exception {
        JsonNode spec = especificacion();

        assertThat(spec.at("/components/schemas/ErrorResponseDTO").isMissingNode()).isFalse();
        JsonNode noEncontrada = spec.at("/paths/~1donaciones~1{id}/get/responses/404/content/application~1json/schema/$ref");
        assertThat(noEncontrada.asText()).isEqualTo("#/components/schemas/ErrorResponseDTO");
        assertThat(spec.at("/paths/~1donantes/get/responses/500").isMissingNode()).isFalse();
    }

    @Test
    @DisplayName("/donantes y /logistica/eventos están documentados")
    void endpointsDocumentados() throws Exception {
        JsonNode spec = especificacion();

        assertThat(spec.at("/paths/~1donantes/post/summary").asText()).isNotBlank();
        assertThat(spec.at("/paths/~1donantes~1import/post/summary").asText()).isNotBlank();
        assertThat(spec.at("/paths/~1logistica~1eventos~1inicio-ruta/post/summary").asText()).isNotBlank();
        assertThat(spec.at("/paths/~1logistica~1eventos~1entrega-fallida/post/responses/404").isMissingNode()).isFalse();
    }
}
