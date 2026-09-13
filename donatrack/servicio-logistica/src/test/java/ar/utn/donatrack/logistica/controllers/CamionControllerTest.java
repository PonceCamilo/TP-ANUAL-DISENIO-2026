package ar.utn.donatrack.logistica.controllers;

import ar.utn.donatrack.logistica.dtos.response.CamionResponseDTO;
import ar.utn.donatrack.logistica.dtos.response.RutaResponseDTO;
import ar.utn.donatrack.logistica.exceptions.CamionNoEncontradoException;
import ar.utn.donatrack.logistica.exceptions.RutaNoEncontradaException;
import ar.utn.donatrack.logistica.interfaces.services.CamionServiceInterface;
import ar.utn.donatrack.logistica.interfaces.services.PlanificacionServiceInterface;
import ar.utn.donatrack.logistica.models.flota.EstadoCamion;
import ar.utn.donatrack.logistica.models.planificacion.EstadoRuta;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de la capa REST de la flota.
 *
 * El GET de ruta vigente vive acá (app del chofer) aunque la lógica esté en
 * el service de planificación: se mockean las dos interfaces que inyecta el
 * controller.
 */
@WebMvcTest(CamionController.class)
@DisplayName("CamionController - contrato HTTP de /api/logistica/camiones")
class CamionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CamionServiceInterface camionService;

    @MockitoBean
    private PlanificacionServiceInterface planificacionService;

    private final UUID idCamion = UUID.randomUUID();

    private CamionResponseDTO respuestaCamion() {
        return CamionResponseDTO.builder()
                .id(idCamion)
                .patente("AB123CD")
                .capacidadVolumenM3(20.0)
                .alturaM(2.5)
                .capacidadCargaKg(1500.0)
                .estado(EstadoCamion.DISPONIBLE)
                .build();
    }

    private String cuerpoAlta() throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "patente", "AB123CD",
                "capacidadVolumenM3", 20.0,
                "alturaM", 2.5,
                "capacidadCargaKg", 1500.0));
    }

    @Nested
    @DisplayName("POST /api/logistica/camiones")
    class Alta {

        @Test
        @DisplayName("Devuelve 201 con el camión creado")
        void altaOk() throws Exception {
            when(camionService.registrar(any())).thenReturn(respuestaCamion());

            mockMvc.perform(post("/api/logistica/camiones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoAlta()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.patente").value("AB123CD"))
                    .andExpect(jsonPath("$.estado").value("DISPONIBLE"));
        }

        @Test
        @DisplayName("Devuelve 400 si falta la patente")
        void sinPatente() throws Exception {
            mockMvc.perform(post("/api/logistica/camiones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "patente", "",
                                    "capacidadVolumenM3", 20.0,
                                    "alturaM", 2.5,
                                    "capacidadCargaKg", 1500.0))))
                    .andExpect(status().isBadRequest());

            verify(camionService, never()).registrar(any());
        }
    }

    @Nested
    @DisplayName("GET /api/logistica/camiones")
    class Consulta {

        @Test
        @DisplayName("GET listado devuelve 200")
        void listadoOk() throws Exception {
            when(camionService.obtenerTodos()).thenReturn(List.of(respuestaCamion()));

            mockMvc.perform(get("/api/logistica/camiones"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)));
        }

        @Test
        @DisplayName("GET por id devuelve 200 con el detalle")
        void detalleOk() throws Exception {
            when(camionService.obtenerPorId(idCamion)).thenReturn(respuestaCamion());

            mockMvc.perform(get("/api/logistica/camiones/{id}", idCamion))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(idCamion.toString()));
        }

        @Test
        @DisplayName("GET por id devuelve 404 si el camión no existe")
        void detalle404() throws Exception {
            when(camionService.obtenerPorId(idCamion)).thenThrow(new CamionNoEncontradoException(idCamion));

            mockMvc.perform(get("/api/logistica/camiones/{id}", idCamion))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/logistica/camiones/{id}/ruta")
    class RutaVigente {

        @Test
        @DisplayName("Devuelve 200 con la ruta vigente del camión")
        void rutaOk() throws Exception {
            when(planificacionService.obtenerRutaVigentePorCamion(idCamion))
                    .thenReturn(RutaResponseDTO.builder()
                            .id(UUID.randomUUID())
                            .camionId(idCamion)
                            .estado(EstadoRuta.INICIADA)
                            .paradas(List.of())
                            .build());

            mockMvc.perform(get("/api/logistica/camiones/{id}/ruta", idCamion))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.camionId").value(idCamion.toString()))
                    .andExpect(jsonPath("$.estado").value("INICIADA"));
        }

        @Test
        @DisplayName("Devuelve 404 si el camión no tiene ruta vigente")
        void ruta404() throws Exception {
            when(planificacionService.obtenerRutaVigentePorCamion(idCamion))
                    .thenThrow(new RutaNoEncontradaException(idCamion));

            mockMvc.perform(get("/api/logistica/camiones/{id}/ruta", idCamion))
                    .andExpect(status().isNotFound());
        }
    }
}
