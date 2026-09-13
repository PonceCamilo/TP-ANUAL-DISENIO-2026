package ar.utn.donatrack.logistica.controllers;

import ar.utn.donatrack.logistica.dtos.response.EntregaResponseDTO;
import ar.utn.donatrack.logistica.exceptions.EntregaNoEncontradaException;
import ar.utn.donatrack.logistica.exceptions.TransicionEntregaIlegalException;
import ar.utn.donatrack.logistica.interfaces.services.EntregaServiceInterface;
import ar.utn.donatrack.logistica.models.entrega.EstadoEntrega;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de la capa REST de entregas.
 *
 * Levantan SOLO la capa web (@WebMvcTest): el service se mockea. Se verifica
 * el contrato HTTP (ruta, verbo, código) y que cada excepción de dominio se
 * traduzca al status que hoy define GlobalExceptionHandler (409 en transiciones
 * ilegales, no 422).
 */
@WebMvcTest(EntregaController.class)
@DisplayName("EntregaController - contrato HTTP de /api/logistica/entregas")
class EntregaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EntregaServiceInterface entregaService;

    private final UUID idEntrega = UUID.randomUUID();

    private EntregaResponseDTO respuesta(EstadoEntrega estado) {
        return EntregaResponseDTO.builder()
                .id(idEntrega)
                .idDonacion(UUID.randomUUID())
                .estado(estado)
                .fotosComprobante(List.of())
                .historial(List.of())
                .build();
    }

    @Nested
    @DisplayName("GET /api/logistica/entregas")
    class Consulta {

        @Test
        @DisplayName("GET por id devuelve 200 con el detalle")
        void detalleOk() throws Exception {
            when(entregaService.obtenerPorId(idEntrega)).thenReturn(respuesta(EstadoEntrega.EN_TRASLADO));

            mockMvc.perform(get("/api/logistica/entregas/{id}", idEntrega))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(idEntrega.toString()))
                    .andExpect(jsonPath("$.estado").value("EN_TRASLADO"));
        }

        @Test
        @DisplayName("GET por id devuelve 404 si la entrega no existe")
        void detalle404() throws Exception {
            when(entregaService.obtenerPorId(idEntrega))
                    .thenThrow(new EntregaNoEncontradaException(idEntrega));

            mockMvc.perform(get("/api/logistica/entregas/{id}", idEntrega))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("GET con filtro de estado pasa el enum al service")
        void filtraPorEstado() throws Exception {
            when(entregaService.obtenerPorEstado(EstadoEntrega.NO_RECIBIDA))
                    .thenReturn(List.of(respuesta(EstadoEntrega.NO_RECIBIDA)));

            mockMvc.perform(get("/api/logistica/entregas").param("estado", "NO_RECIBIDA"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                    .andExpect(jsonPath("$[0].estado").value("NO_RECIBIDA"));

            verify(entregaService).obtenerPorEstado(EstadoEntrega.NO_RECIBIDA);
        }
    }

    @Nested
    @DisplayName("POST /api/logistica/entregas/{id}/confirmar")
    class Confirmar {

        @Test
        @DisplayName("Devuelve 200 y delega en el service")
        void confirmarOk() throws Exception {
            mockMvc.perform(post("/api/logistica/entregas/{id}/confirmar", idEntrega)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("fotosComprobante", List.of("foto1.jpg")))))
                    .andExpect(status().isOk());

            verify(entregaService).confirmar(eq(idEntrega), any());
        }

        @Test
        @DisplayName("Devuelve 400 si la lista de fotos viene vacía")
        void sinFotos() throws Exception {
            mockMvc.perform(post("/api/logistica/entregas/{id}/confirmar", idEntrega)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("fotosComprobante", List.of()))))
                    .andExpect(status().isBadRequest());

            verify(entregaService, never()).confirmar(any(), any());
        }

        @Test
        @DisplayName("Devuelve 409 si la transición no está permitida")
        void transicionIlegal() throws Exception {
            doThrow(new TransicionEntregaIlegalException(EstadoEntrega.LISTO_PARA_ENTREGAR, EstadoEntrega.ENTREGADA))
                    .when(entregaService).confirmar(eq(idEntrega), any());

            mockMvc.perform(post("/api/logistica/entregas/{id}/confirmar", idEntrega)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("fotosComprobante", List.of("foto1.jpg")))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));
        }
    }

    @Nested
    @DisplayName("POST /api/logistica/entregas/{id}/no-recibida")
    class NoRecibida {

        @Test
        @DisplayName("Devuelve 200 y delega en el service")
        void noRecibidaOk() throws Exception {
            mockMvc.perform(post("/api/logistica/entregas/{id}/no-recibida", idEntrega)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("motivo", "ENTIDAD_AUSENTE"))))
                    .andExpect(status().isOk());

            verify(entregaService).marcarNoRecibida(eq(idEntrega), any());
        }

        @Test
        @DisplayName("Devuelve 400 si falta el motivo")
        void sinMotivo() throws Exception {
            mockMvc.perform(post("/api/logistica/entregas/{id}/no-recibida", idEntrega)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());

            verify(entregaService, never()).marcarNoRecibida(any(), any());
        }

        @Test
        @DisplayName("Devuelve 409 si la entrega no está EN_TRASLADO")
        void transicionIlegal() throws Exception {
            doThrow(new TransicionEntregaIlegalException(EstadoEntrega.LISTO_PARA_ENTREGAR, EstadoEntrega.NO_RECIBIDA))
                    .when(entregaService).marcarNoRecibida(eq(idEntrega), any());

            mockMvc.perform(post("/api/logistica/entregas/{id}/no-recibida", idEntrega)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("motivo", "ENTIDAD_AUSENTE"))))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("POST /api/logistica/entregas/{id}/regreso-deposito")
    class RegresoADeposito {

        @Test
        @DisplayName("Devuelve 200 y delega en el service")
        void regresoOk() throws Exception {
            mockMvc.perform(post("/api/logistica/entregas/{id}/regreso-deposito", idEntrega))
                    .andExpect(status().isOk());

            verify(entregaService).regresarADeposito(idEntrega);
        }

        @Test
        @DisplayName("Devuelve 409 si la entrega no está NO_RECIBIDA")
        void transicionIlegal() throws Exception {
            doThrow(new TransicionEntregaIlegalException(EstadoEntrega.ENTREGADA, EstadoEntrega.LISTO_PARA_ENTREGAR))
                    .when(entregaService).regresarADeposito(idEntrega);

            mockMvc.perform(post("/api/logistica/entregas/{id}/regreso-deposito", idEntrega))
                    .andExpect(status().isConflict());
        }
    }
}
