package ar.utn.donatrack.logistica.controllers;

import ar.utn.donatrack.logistica.interfaces.services.PlanificacionServiceInterface;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de la capa REST de rutas (inicio).
 *
 * El inicio de ruta es el disparador del evento INICIO_RUTA hacia el broker: acá
 * solo se verifica que el verbo y el código HTTP coincidan; la publicación
 * está cubierta en PlanificacionRutasServiceTest.
 */
@WebMvcTest(RutaController.class)
@DisplayName("RutaController - contrato HTTP de /api/logistica/rutas")
class RutaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlanificacionServiceInterface planificacionService;

    private final UUID idRuta = UUID.randomUUID();

    @Nested
    @DisplayName("POST /api/logistica/rutas/{id}/iniciar")
    class Iniciar {

        @Test
        @DisplayName("Devuelve 200 y delega en el service")
        void iniciarOk() throws Exception {
            mockMvc.perform(post("/api/logistica/rutas/{id}/iniciar", idRuta))
                    .andExpect(status().isOk());

            verify(planificacionService).iniciarRuta(idRuta);
        }
    }
}
