package ar.utn.donatrack.donaciones.controllers;

import ar.utn.donatrack.donaciones.services.EntidadesBeneficiariasService;
import ar.utn.donatrack.donaciones.dtos.response.CampaniaResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.EntidadBeneficiariaResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.NecesidadExtraordinariaResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.NecesidadRecurrenteResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.NecesidadResponseDTO;
import ar.utn.donatrack.donaciones.exceptions.entidadesExceptions.CambioTipoNecesidadException;
import ar.utn.donatrack.donaciones.exceptions.entidadesExceptions.CampaniaNoEncontradaException;
import ar.utn.donatrack.donaciones.exceptions.entidadesExceptions.EntidadBeneficiariaNoEncontradaException;
import ar.utn.donatrack.donaciones.exceptions.entidadesExceptions.FechasCampaniaInvalidasException;
import ar.utn.donatrack.donaciones.exceptions.entidadesExceptions.NecesidadNoEncontradaException;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.periodicidades.Periodicidad;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de la capa REST de entidades beneficiarias.
 *
 * Es el controller con la jerarquía de rutas más profunda del servicio:
 *   /entidades/{entidadId}/campanias/{campaniaId}/necesidades/{necesidadId}
 *
 * Por eso los tests insisten en que cada nivel del recorrido devuelva SU propio
 * 404 (entidad, campaña o necesidad), y no un 404 genérico que obligue al
 * cliente a adivinar qué parte de la ruta está mal.
 *
 * También se verifica el polimorfismo del JSON de necesidades: el campo "tipo"
 * (EXTRAORDINARIA / RECURRENTE) decide el subtipo tanto al recibir como al
 * devolver, y que el tipo NO se pueda cambiar en un PUT.
 */
@WebMvcTest(EntidadBeneficiariaController.class)
@DisplayName("EntidadBeneficiariaController - contrato HTTP de /entidades")
class EntidadBeneficiariaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EntidadesBeneficiariasService entidadesService;

    private final UUID idEntidad = UUID.randomUUID();
    private final UUID idCampania = UUID.randomUUID();
    private final UUID idNecesidad = UUID.randomUUID();

    // ── Respuestas de ejemplo ────────────────────────────────────────────────

    private EntidadBeneficiariaResponseDTO respuestaEntidad() {
        return EntidadBeneficiariaResponseDTO.builder()
                .id(idEntidad)
                .razonSocial("Comedor Los Pibes")
                .contactos(List.of())
                .representantes(List.of())
                .campanias(List.of())
                .build();
    }

    private CampaniaResponseDTO respuestaCampania() {
        return CampaniaResponseDTO.builder()
                .idCampania(idCampania)
                .idEntidad(idEntidad)
                .descripcionGeneral("Colecta post inundación")
                .fechaInicio(LocalDate.now())
                .fechaFin(LocalDate.now().plusMonths(1))
                .necesidades(List.of())
                .build();
    }

    private NecesidadResponseDTO respuestaExtraordinaria() {
        return NecesidadExtraordinariaResponseDTO.builder()
                .id(idNecesidad)
                .nombre("Colchones")
                .descripcion("Colchones de una plaza")
                .cantidadObjetivo(50)
                .cantidadRecibida(0)
                .satisfecha(false)
                .build();
    }

    private NecesidadResponseDTO respuestaRecurrente() {
        return NecesidadRecurrenteResponseDTO.builder()
                .id(idNecesidad)
                .nombre("Fideos")
                .cantidadObjetivo(100)
                .cantidadRecibida(0)
                .satisfecha(false)
                .periodo(Periodicidad.SEMANAL)
                .fechaInicioPeriodo(LocalDate.now())
                .build();
    }

    // ── Cuerpos de petición ──────────────────────────────────────────────────

    /** Cuerpo válido de alta/actualización de entidad, con su dirección anidada. */
    private String cuerpoEntidad(String razonSocial) throws Exception {
        Map<String, Object> direccion = new LinkedHashMap<>();
        direccion.put("calle", "Medrano");
        direccion.put("numero", 951);
        direccion.put("codigoPostal", "C1179");
        direccion.put("localidad", Map.of("nombre", "CABA", "provincia", Map.of("nombre", "Buenos Aires")));

        return objectMapper.writeValueAsString(Map.of(
                "razonSocial", razonSocial,
                "direccion", direccion));
    }

    private String cuerpoCampania(LocalDate inicio, LocalDate fin) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "descripcionGeneral", "Colecta post inundación",
                "fechaInicio", inicio.toString(),
                "fechaFin", fin.toString()));
    }

    private String cuerpoNecesidadExtraordinaria(int objetivo) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "tipo", "EXTRAORDINARIA",
                "nombre", "Colchones",
                "descripcion", "Colchones de una plaza",
                "cantidadObjetivo", objetivo));
    }

    private String cuerpoNecesidadRecurrente() throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "tipo", "RECURRENTE",
                "nombre", "Fideos",
                "cantidadObjetivo", 100,
                "periodo", "SEMANAL"));
    }

    @Nested
    @DisplayName("CRUD de entidades")
    class CrudEntidades {

        @Test
        @DisplayName("POST devuelve 201 con la entidad creada y la cabecera Location")
        void altaOk() throws Exception {
            when(entidadesService.guardar(any())).thenReturn(respuestaEntidad());

            mockMvc.perform(post("/entidades")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoEntidad("Comedor Los Pibes")))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/entidades/" + idEntidad))
                    .andExpect(jsonPath("$.razonSocial").value("Comedor Los Pibes"));
        }

        @Test
        @DisplayName("POST devuelve 400 si falta la razón social")
        void altaSinRazonSocial() throws Exception {
            String cuerpo = objectMapper.writeValueAsString(Map.of(
                    "direccion", Map.of("calle", "Medrano", "numero", 951, "codigoPostal", "C1179",
                            "localidad", Map.of("nombre", "CABA", "provincia", Map.of("nombre", "Buenos Aires")))));

            mockMvc.perform(post("/entidades")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isBadRequest());

            verify(entidadesService, never()).guardar(any());
        }

        @Test
        @DisplayName("GET devuelve 200 con el listado de entidades")
        void listadoOk() throws Exception {
            when(entidadesService.obtenerTodas()).thenReturn(List.of(respuestaEntidad()));

            mockMvc.perform(get("/entidades"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)));
        }

        @Test
        @DisplayName("GET por id devuelve 200 con el detalle")
        void detalleOk() throws Exception {
            when(entidadesService.obtenerPorId(idEntidad)).thenReturn(respuestaEntidad());

            mockMvc.perform(get("/entidades/{id}", idEntidad))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(idEntidad.toString()));
        }

        @Test
        @DisplayName("GET por id devuelve 404 si la entidad no existe")
        void detalle404() throws Exception {
            when(entidadesService.obtenerPorId(idEntidad))
                    .thenThrow(new EntidadBeneficiariaNoEncontradaException(idEntidad));

            mockMvc.perform(get("/entidades/{id}", idEntidad))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("PUT devuelve 204 al actualizar la entidad")
        void actualizarOk() throws Exception {
            mockMvc.perform(put("/entidades/{id}", idEntidad)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoEntidad("Comedor Renovado")))
                    .andExpect(status().isNoContent());

            verify(entidadesService).actualizar(eq(idEntidad), any());
        }

        @Test
        @DisplayName("DELETE devuelve 204 al eliminar la entidad")
        void bajaOk() throws Exception {
            mockMvc.perform(delete("/entidades/{id}", idEntidad))
                    .andExpect(status().isNoContent());

            verify(entidadesService).eliminarEntidad(idEntidad);
        }

        @Test
        @DisplayName("DELETE devuelve 404 si la entidad no existe")
        void baja404() throws Exception {
            doThrow(new EntidadBeneficiariaNoEncontradaException(idEntidad))
                    .when(entidadesService).eliminarEntidad(idEntidad);

            mockMvc.perform(delete("/entidades/{id}", idEntidad))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Campañas de una entidad")
    class Campanias {

        @Test
        @DisplayName("POST devuelve 201 con la campaña creada")
        void altaCampaniaOk() throws Exception {
            when(entidadesService.agregarCampaniaAEntidad(eq(idEntidad), any())).thenReturn(respuestaCampania());

            mockMvc.perform(post("/entidades/{id}/campanias", idEntidad)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoCampania(LocalDate.now(), LocalDate.now().plusMonths(1))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.idCampania").value(idCampania.toString()))
                    .andExpect(jsonPath("$.idEntidad").value(idEntidad.toString()));
        }

        @Test
        @DisplayName("POST devuelve 400 si la fecha de inicio es anterior a hoy")
        void fechaInicioEnElPasado() throws Exception {
            // @FutureOrPresent en el DTO: no tiene sentido dar de alta una campaña
            // que ya arrancó antes de cargarla.
            mockMvc.perform(post("/entidades/{id}/campanias", idEntidad)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoCampania(LocalDate.now().minusDays(5), LocalDate.now().plusMonths(1))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST devuelve 400 si el inicio es posterior al fin")
        void fechasInvertidas() throws Exception {
            doThrow(new FechasCampaniaInvalidasException("La fecha de inicio no puede ser posterior a la de fin"))
                    .when(entidadesService).agregarCampaniaAEntidad(eq(idEntidad), any());

            mockMvc.perform(post("/entidades/{id}/campanias", idEntidad)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoCampania(LocalDate.now().plusMonths(2), LocalDate.now().plusMonths(1))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST devuelve 404 si la entidad no existe")
        void entidadInexistente() throws Exception {
            when(entidadesService.agregarCampaniaAEntidad(eq(idEntidad), any()))
                    .thenThrow(new EntidadBeneficiariaNoEncontradaException(idEntidad));

            mockMvc.perform(post("/entidades/{id}/campanias", idEntidad)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoCampania(LocalDate.now(), LocalDate.now().plusMonths(1))))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Alta y consulta de necesidades")
    class AltaYConsultaDeNecesidades {

        private static final String RUTA = "/entidades/{entidadId}/campanias/{campaniaId}/necesidades";

        @Test
        @DisplayName("POST devuelve 201 con la necesidad extraordinaria creada y su discriminador de tipo")
        void altaExtraordinaria() throws Exception {
            when(entidadesService.agregarNecesidadACampania(eq(idEntidad), eq(idCampania), any()))
                    .thenReturn(respuestaExtraordinaria());

            mockMvc.perform(post(RUTA, idEntidad, idCampania)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoNecesidadExtraordinaria(50)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tipo").value("EXTRAORDINARIA"))
                    .andExpect(jsonPath("$.nombre").value("Colchones"))
                    .andExpect(jsonPath("$.cantidadRecibida").value(0));
        }

        @Test
        @DisplayName("POST devuelve 201 con la necesidad recurrente, incluyendo periodicidad y período")
        void altaRecurrente() throws Exception {
            when(entidadesService.agregarNecesidadACampania(eq(idEntidad), eq(idCampania), any()))
                    .thenReturn(respuestaRecurrente());

            mockMvc.perform(post(RUTA, idEntidad, idCampania)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoNecesidadRecurrente()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tipo").value("RECURRENTE"))
                    .andExpect(jsonPath("$.periodo").value("SEMANAL"))
                    .andExpect(jsonPath("$.fechaInicioPeriodo").exists());
        }

        @Test
        @DisplayName("POST devuelve 400 si el JSON no indica el tipo de necesidad")
        void faltaElDiscriminador() throws Exception {
            // Sin "tipo" Jackson no puede elegir el subtipo de DTO.
            mockMvc.perform(post(RUTA, idEntidad, idCampania)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "nombre", "Colchones", "cantidadObjetivo", 50))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST devuelve 400 si la cantidad objetivo no es mayor a 0")
        void cantidadObjetivoInvalida() throws Exception {
            mockMvc.perform(post(RUTA, idEntidad, idCampania)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoNecesidadExtraordinaria(0)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST devuelve 400 si una necesidad recurrente no indica periodicidad")
        void recurrenteSinPeriodicidad() throws Exception {
            mockMvc.perform(post(RUTA, idEntidad, idCampania)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "tipo", "RECURRENTE", "nombre", "Fideos", "cantidadObjetivo", 100))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST devuelve 404 si la campaña no pertenece a la entidad")
        void campaniaInexistente() throws Exception {
            when(entidadesService.agregarNecesidadACampania(eq(idEntidad), eq(idCampania), any()))
                    .thenThrow(new CampaniaNoEncontradaException(idCampania, idEntidad));

            mockMvc.perform(post(RUTA, idEntidad, idCampania)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoNecesidadExtraordinaria(50)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET devuelve 200 con las necesidades de la campaña")
        void listadoOk() throws Exception {
            when(entidadesService.obtenerNecesidadesDeCampania(idEntidad, idCampania))
                    .thenReturn(List.of(respuestaExtraordinaria(), respuestaRecurrente()));

            mockMvc.perform(get(RUTA, idEntidad, idCampania))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
                    .andExpect(jsonPath("$[0].tipo").value("EXTRAORDINARIA"))
                    .andExpect(jsonPath("$[1].tipo").value("RECURRENTE"));
        }

        @Test
        @DisplayName("GET por id devuelve 200 con la necesidad puntual")
        void detalleOk() throws Exception {
            when(entidadesService.obtenerNecesidad(idEntidad, idCampania, idNecesidad))
                    .thenReturn(respuestaExtraordinaria());

            mockMvc.perform(get(RUTA + "/{necesidadId}", idEntidad, idCampania, idNecesidad))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(idNecesidad.toString()));
        }

        @Test
        @DisplayName("GET por id devuelve 404 si la necesidad no está en esa campaña")
        void detalle404() throws Exception {
            when(entidadesService.obtenerNecesidad(idEntidad, idCampania, idNecesidad))
                    .thenThrow(new NecesidadNoEncontradaException(idNecesidad, idCampania));

            mockMvc.perform(get(RUTA + "/{necesidadId}", idEntidad, idCampania, idNecesidad))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Modificación y baja de necesidades")
    class ModificacionYBajaDeNecesidades {

        private static final String RUTA =
                "/entidades/{entidadId}/campanias/{campaniaId}/necesidades/{necesidadId}";

        @Test
        @DisplayName("PUT devuelve 200 con la necesidad actualizada")
        void actualizarOk() throws Exception {
            when(entidadesService.actualizarNecesidad(eq(idEntidad), eq(idCampania), eq(idNecesidad), any()))
                    .thenReturn(respuestaExtraordinaria());

            mockMvc.perform(put(RUTA, idEntidad, idCampania, idNecesidad)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoNecesidadExtraordinaria(80)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(idNecesidad.toString()));
        }

        @Test
        @DisplayName("PUT devuelve 400 al intentar cambiar el tipo de la necesidad")
        void rechazaCambioDeTipo() throws Exception {
            // Convertir una extraordinaria en recurrente cambiaría el significado
            // de lo ya recibido, así que el sistema obliga a borrar y recrear.
            when(entidadesService.actualizarNecesidad(eq(idEntidad), eq(idCampania), eq(idNecesidad), any()))
                    .thenThrow(new CambioTipoNecesidadException("EXTRAORDINARIA", "RECURRENTE"));

            mockMvc.perform(put(RUTA, idEntidad, idCampania, idNecesidad)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoNecesidadRecurrente()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("PUT devuelve 404 si la necesidad no existe")
        void actualizar404() throws Exception {
            when(entidadesService.actualizarNecesidad(eq(idEntidad), eq(idCampania), eq(idNecesidad), any()))
                    .thenThrow(new NecesidadNoEncontradaException(idNecesidad, idCampania));

            mockMvc.perform(put(RUTA, idEntidad, idCampania, idNecesidad)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoNecesidadExtraordinaria(80)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("DELETE devuelve 204 al eliminar la necesidad")
        void bajaOk() throws Exception {
            mockMvc.perform(delete(RUTA, idEntidad, idCampania, idNecesidad))
                    .andExpect(status().isNoContent());

            verify(entidadesService).eliminarNecesidad(idEntidad, idCampania, idNecesidad);
        }

        @Test
        @DisplayName("DELETE devuelve 404 en lugar de borrar en silencio si la necesidad no existe")
        void baja404() throws Exception {
            doThrow(new NecesidadNoEncontradaException(idNecesidad, idCampania))
                    .when(entidadesService).eliminarNecesidad(idEntidad, idCampania, idNecesidad);

            mockMvc.perform(delete(RUTA, idEntidad, idCampania, idNecesidad))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("DELETE devuelve 404 propio de la campaña cuando el id de campaña es el que está mal")
        void baja404DeCampania() throws Exception {
            // Cada nivel de la ruta tiene su propio 404: así el cliente sabe si el
            // problema es la entidad, la campaña o la necesidad.
            doThrow(new CampaniaNoEncontradaException(idCampania, idEntidad))
                    .when(entidadesService).eliminarNecesidad(idEntidad, idCampania, idNecesidad);

            mockMvc.perform(delete(RUTA, idEntidad, idCampania, idNecesidad))
                    .andExpect(status().isNotFound());
        }
    }
}
