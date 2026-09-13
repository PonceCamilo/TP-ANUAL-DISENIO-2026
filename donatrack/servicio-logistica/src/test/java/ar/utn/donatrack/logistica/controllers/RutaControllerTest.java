package ar.utn.donatrack.logistica.controllers;

import ar.utn.donatrack.logistica.dtos.response.RutaResponseDTO;
import ar.utn.donatrack.logistica.exceptions.RutaNoEncontradaException;
import ar.utn.donatrack.logistica.interfaces.services.PlanificacionServiceInterface;
import ar.utn.donatrack.logistica.models.planificacion.EstadoRuta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de la capa REST de rutas (consulta e inicio).
 *
 * El inicio de ruta es el disparador del evento INICIO_RUTA hacia n8n: acá
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

    private RutaResponseDTO respuesta() {
        return RutaResponseDTO.builder()
                .id(idRuta)
                .camionId(UUID.randomUUID())
                .estado(EstadoRuta.PLANIFICADA)
                .paradas(List.of())
                .build();
    }

    @Nested
    @DisplayName("GET /api/logistica/rutas/{id}")
    class Detalle {

        @Test
        @DisplayName("Devuelve 200 con la ruta")
        void detalleOk() throws Exception {
            when(planificacionService.obtenerRuta(idRuta)).thenReturn(respuesta());

            mockMvc.perform(get("/api/logistica/rutas/{id}", idRuta))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(idRuta.toString()))
                    .andExpect(jsonPath("$.estado").value("PLANIFICADA"));
        }

        @Test
        @DisplayName("Devuelve 404 si la ruta no existe")
        void detalle404() throws Exception {
            when(planificacionService.obtenerRuta(idRuta)).thenThrow(new RutaNoEncontradaException(idRuta));

            mockMvc.perform(get("/api/logistica/rutas/{id}", idRuta))
                    .andExpect(status().isNotFound());
        }
    }

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

        @Test
        @DisplayName("Devuelve 404 si la ruta no existe")
        void iniciar404() throws Exception {
            doThrow(new RutaNoEncontradaException(idRuta)).when(planificacionService).iniciarRuta(idRuta);

            mockMvc.perform(post("/api/logistica/rutas/{id}/iniciar", idRuta))
                    .andExpect(status().isNotFound());
        }
    }
}
