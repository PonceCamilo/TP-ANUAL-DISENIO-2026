package ar.utn.donatrack.logistica.controllers;

import ar.utn.donatrack.logistica.dtos.response.LoteResponseDTO;
import ar.utn.donatrack.logistica.exceptions.LoteCallbackInvalidoException;
import ar.utn.donatrack.logistica.exceptions.ProveedorRuteoIndisponibleException;
import ar.utn.donatrack.logistica.interfaces.services.PlanificacionServiceInterface;
import ar.utn.donatrack.logistica.models.planificacion.EstadoLote;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de la capa REST de planificaciones.
 *
 * Además del CRUD del lote, cubre el callback asíncrono del proveedor: el
 * token viaja en Authorization (con o sin prefijo Bearer) y un token inválido
 * responde 409. El 503 del proveedor caído también se verifica acá.
 */
@WebMvcTest(PlanificacionController.class)
@DisplayName("PlanificacionController - contrato HTTP de /api/logistica/planificaciones")
class PlanificacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PlanificacionServiceInterface planificacionService;

    private final UUID idLote = UUID.randomUUID();
    private final UUID idCamion = UUID.randomUUID();
    private final UUID idDonacion = UUID.randomUUID();
    private final UUID idEntidad = UUID.randomUUID();

    private LoteResponseDTO respuestaLote() {
        return LoteResponseDTO.builder()
                .id(idLote)
                .estado(EstadoLote.COMPLETADO)
                .cantidadDonaciones(1)
                .rutas(List.of())
                .build();
    }

    private Map<String, Object> direccion() {
        Map<String, Object> direccion = new LinkedHashMap<>();
        direccion.put("calle", "Medrano");
        direccion.put("numero", 951);
        direccion.put("localidad", "CABA");
        direccion.put("provincia", "Buenos Aires");
        direccion.put("codigoPostal", "C1179");
        return direccion;
    }

    private String cuerpoPlanificar() throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "camionesIds", List.of(idCamion),
                "donaciones", List.of(Map.of(
                        "idDonacion", idDonacion,
                        "idEntidadBeneficiaria", idEntidad,
                        "direccionEntrega", direccion()))));
    }

    private String cuerpoCallback() throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "loteId", idLote,
                "rutas", List.of(Map.of(
                        "camionId", idCamion,
                        "paradas", List.of(Map.of(
                                "orden", 1,
                                "idEntidadBeneficiaria", idEntidad,
                                "direccion", direccion(),
                                "donacionesIds", List.of(idDonacion)))))));
    }

    @Nested
    @DisplayName("POST /api/logistica/planificaciones")
    class Planificar {

        @Test
        @DisplayName("Devuelve 200 con los lotes planificados")
        void planificarOk() throws Exception {
            when(planificacionService.planificar(any())).thenReturn(List.of(respuestaLote()));

            mockMvc.perform(post("/api/logistica/planificaciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoPlanificar()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                    .andExpect(jsonPath("$[0].estado").value("COMPLETADO"));
        }

        @Test
        @DisplayName("Devuelve 400 si no se envían donaciones")
        void sinDonaciones() throws Exception {
            mockMvc.perform(post("/api/logistica/planificaciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "camionesIds", List.of(idCamion),
                                    "donaciones", List.of()))))
                    .andExpect(status().isBadRequest());

            verify(planificacionService, never()).planificar(any());
        }

        @Test
        @DisplayName("Devuelve 503 si el proveedor de ruteo no responde")
        void proveedorIndisponible() throws Exception {
            when(planificacionService.planificar(any()))
                    .thenThrow(new ProveedorRuteoIndisponibleException(idCamion, new RuntimeException("timeout")));

            mockMvc.perform(post("/api/logistica/planificaciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoPlanificar()))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.status").value(503));
        }

        @Test
        @DisplayName("Acepta el pedido sin camionesIds (los resuelve logística)")
        void planificarSinCamiones() throws Exception {
            when(planificacionService.planificar(any())).thenReturn(List.of(respuestaLote()));

            mockMvc.perform(post("/api/logistica/planificaciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "donaciones", List.of(Map.of(
                                            "idDonacion", idDonacion,
                                            "idEntidadBeneficiaria", idEntidad,
                                            "direccionEntrega", direccion()))))))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/logistica/planificaciones/callback")
    class Callback {

        @Test
        @DisplayName("Devuelve 200 y pasa el token crudo del header Authorization")
        void callbackConTokenCrudo() throws Exception {
            mockMvc.perform(post("/api/logistica/planificaciones/callback")
                            .header("Authorization", "token-123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoCallback()))
                    .andExpect(status().isOk());

            verify(planificacionService).registrarCallback(any(), eq("token-123"));
        }

        @Test
        @DisplayName("Si el header trae Bearer, se lo recorta antes de delegar")
        void callbackConBearer() throws Exception {
            mockMvc.perform(post("/api/logistica/planificaciones/callback")
                            .header("Authorization", "Bearer token-123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoCallback()))
                    .andExpect(status().isOk());

            verify(planificacionService).registrarCallback(any(), eq("token-123"));
        }

        @Test
        @DisplayName("Devuelve 409 si el token no coincide con el lote")
        void tokenInvalido() throws Exception {
            doThrow(new LoteCallbackInvalidoException(idLote))
                    .when(planificacionService).registrarCallback(any(), any());

            mockMvc.perform(post("/api/logistica/planificaciones/callback")
                            .header("Authorization", "token-equivocado")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoCallback()))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Devuelve 400 si falta el header Authorization")
        void sinAuthorization() throws Exception {
            mockMvc.perform(post("/api/logistica/planificaciones/callback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoCallback()))
                    .andExpect(status().isBadRequest());
        }
    }
}
