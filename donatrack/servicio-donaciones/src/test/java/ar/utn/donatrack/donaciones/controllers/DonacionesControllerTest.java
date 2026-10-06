package ar.utn.donatrack.donaciones.controllers;

import ar.utn.donatrack.donaciones.dtos.request.AsignacionRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.BienRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.CambioEstadoRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.DonacionRequestDTO;
import ar.utn.donatrack.donaciones.dtos.response.CandidatosPendientesResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.DonacionResponseDTO;
import ar.utn.donatrack.donaciones.exceptions.cambioEstadosExceptions.CambioEstadoDonacionIlegalException;
import ar.utn.donatrack.donaciones.exceptions.cambioEstadosExceptions.FaltaJustificacionDonacionException;
import ar.utn.donatrack.donaciones.exceptions.donacionesExceptions.DonacionNoEncontradaException;
import ar.utn.donatrack.donaciones.exceptions.donacionesExceptions.DonacionSinBienesException;
import ar.utn.donatrack.donaciones.exceptions.entidadesExceptions.EntidadBeneficiariaNoEncontradaException;
import ar.utn.donatrack.donaciones.exceptions.personasExceptions.PersonaDonanteNoEncontradaException;
import ar.utn.donatrack.donaciones.interfaces.services.DonacionServiceInterface;
import ar.utn.donatrack.donaciones.interfaces.services.SegmentadorDonacionesServiceInterface;
import ar.utn.donatrack.donaciones.mappers.DonacionMapper;
import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienGenerico;
import ar.utn.donatrack.donaciones.validations.personas.PersonasValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de la capa REST de donaciones.
 *
 * Levantan SOLO la capa web (@WebMvcTest): los services se mockean, así que lo
 * que se prueba acá es el contrato HTTP, no la lógica de negocio (que ya está
 * cubierta en DonacionServiceTest).
 *
 * Se verifican tres cosas por endpoint:
 *   - Ruta, verbo y código de estado correctos (201 al crear, 204 sin contenido).
 *   - Que el cuerpo y los parámetros lleguen bien al service.
 *   - Que cada excepción de dominio se traduzca al HTTP correcto vía el
 *     GlobalExceptionHandler: 404 no encontrado, 409 conflicto, 422 transición
 *     ilegal, 400 datos inválidos.
 */
@WebMvcTest(DonacionesController.class)
@Import(DonacionMapper.class)
@DisplayName("DonacionesController - contrato HTTP de /donaciones")
class DonacionesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DonacionServiceInterface donacionService;

    @MockitoBean
    private SegmentadorDonacionesServiceInterface segmentadorService;

    @MockitoBean
    private PersonasValidator personasValidator;

    private final UUID idDonacion = UUID.randomUUID();
    private final UUID idDonante = UUID.randomUUID();

    /** DTO de respuesta mínimo, suficiente para verificar el JSON devuelto. */
    private DonacionResponseDTO respuesta(String estado, String subcategoria) {
        return DonacionResponseDTO.builder()
                .id(idDonacion)
                .idDonante(idDonante)
                .descripcion("Donación de prueba")
                .subcategoria(subcategoria)
                .estado(estado)
                .bienes(List.of())
                .historialEstados(List.of())
                .build();
    }

    /** Cuerpo válido para registrar una donación de un bien genérico. */
    private DonacionRequestDTO requestValido() {
        BienRequestDTO bien = new BienRequestDTO();
        bien.setSubcategoria("arroz");
        bien.setDescripcion("Arroz largo fino");
        bien.setCantidad(10);
        bien.setUnidad("kg");
        return new DonacionRequestDTO(idDonante, "Colecta de alimentos", List.of(bien));
    }

    private Donacion donacionSegmentada() {
        Donacion donacion = new Donacion();
        donacion.setIdDonante(idDonante);
        donacion.setDescripcion("Colecta de alimentos");
        donacion.setSubcategoria(new Subcategoria("arroz"));
        donacion.setBienes(List.of(BienGenerico.builder()
                .subcategoria(new Subcategoria("arroz"))
                .descripcion("Arroz largo fino")
                .cantidad(10)
                .unidad("kg")
                .build()));
        return donacion;
    }

    @Nested
    @DisplayName("GET /donaciones - listado")
    class Listado {

        @Test
        @DisplayName("Devuelve 200 con la lista de donaciones")
        void listaDonaciones() throws Exception {
            when(donacionService.obtenerDonaciones(isNull(), isNull(), isNull()))
                    .thenReturn(List.of(respuesta("EN_DEPOSITO", "arroz")));

            mockMvc.perform(get("/donaciones"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                    .andExpect(jsonPath("$[0].estado").value("EN_DEPOSITO"))
                    .andExpect(jsonPath("$[0].subcategoria").value("arroz"));
        }

        @Test
        @DisplayName("Pasa los tres query params al service tal como llegan")
        void pasaLosFiltros() throws Exception {
            when(donacionService.obtenerDonaciones("EN_DEPOSITO", idDonante, "arroz")).thenReturn(List.of());

            mockMvc.perform(get("/donaciones")
                            .param("estado", "EN_DEPOSITO")
                            .param("idDonante", idDonante.toString())
                            .param("subcategoria", "arroz"))
                    .andExpect(status().isOk());

            verify(donacionService).obtenerDonaciones("EN_DEPOSITO", idDonante, "arroz");
        }

        @Test
        @DisplayName("Devuelve 200 con lista vacía cuando no hay donaciones")
        void listaVacia() throws Exception {
            when(donacionService.obtenerDonaciones(isNull(), isNull(), isNull())).thenReturn(List.of());

            mockMvc.perform(get("/donaciones"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /donaciones/{id} - detalle")
    class Detalle {

        @Test
        @DisplayName("Devuelve 200 con el detalle de la donación")
        void detalleOk() throws Exception {
            when(donacionService.obtenerPorId(idDonacion)).thenReturn(respuesta("EN_DEPOSITO", "arroz"));

            mockMvc.perform(get("/donaciones/{id}", idDonacion))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(idDonacion.toString()));
        }

        @Test
        @DisplayName("Devuelve 404 si la donación no existe")
        void detalle404() throws Exception {
            when(donacionService.obtenerPorId(idDonacion)).thenThrow(new DonacionNoEncontradaException(idDonacion));

            mockMvc.perform(get("/donaciones/{id}", idDonacion))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("Un id que no es UUID hoy devuelve 500 (hueco conocido del manejador de errores)")
        void idMalFormado() throws Exception {
            // COMPORTAMIENTO ACTUAL, no el deseable. Spring lanza
            // MethodArgumentTypeMismatchException, que GlobalExceptionHandler no
            // maneja de forma específica, así que cae en el catch-all de Exception
            // y responde 500. Lo correcto sería 400 (el cliente mandó mal el id).
            // El test deja el hueco documentado: si se agrega el handler, este
            // test falla y hay que cambiarlo a isBadRequest().
            mockMvc.perform(get("/donaciones/{id}", "no-es-un-uuid"))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("POST /donaciones - registro y segmentación")
    class Registro {

        @Test
        @DisplayName("Devuelve 201 con las donaciones ya segmentadas")
        void registroOk() throws Exception {
            // El POST devuelve el cuerpo con las donaciones creadas para que el
            // cliente conozca los ids generados por la segmentación.
            doNothing().when(personasValidator).validarExistenciaPersona(idDonante);
            when(segmentadorService.segmentar(any())).thenReturn(List.of(donacionSegmentada()));

            mockMvc.perform(post("/donaciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestValido())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                    .andExpect(jsonPath("$[0].id").exists())
                    .andExpect(jsonPath("$[0].estado").value("EN_DEPOSITO"))
                    .andExpect(jsonPath("$[0].subcategoria").value("arroz"));
        }

        @Test
        @DisplayName("Devuelve 404 si el donante indicado no existe, sin segmentar nada")
        void donanteInexistente() throws Exception {
            doThrow(new PersonaDonanteNoEncontradaException(idDonante))
                    .when(personasValidator).validarExistenciaPersona(idDonante);

            mockMvc.perform(post("/donaciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestValido())))
                    .andExpect(status().isNotFound());

            verify(segmentadorService, never()).segmentar(any());
        }

        @Test
        @DisplayName("Devuelve 400 si falta el donante en el cuerpo")
        void faltaElDonante() throws Exception {
            // Lo rechaza @Valid (@NotNull en el DTO) antes de llegar al service.
            DonacionRequestDTO sinDonante = requestValido();
            sinDonante.setIdDonante(null);

            mockMvc.perform(post("/donaciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sinDonante)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Devuelve 400 si la lista de bienes viene vacía")
        void sinBienes() throws Exception {
            DonacionRequestDTO sinBienes = requestValido();
            sinBienes.setBienes(List.of());

            mockMvc.perform(post("/donaciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sinBienes)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Devuelve 400 si el JSON está mal formado")
        void jsonInvalido() throws Exception {
            mockMvc.perform(post("/donaciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ esto no es json valido"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PATCH /donaciones/{id}/estado - cambio de estado")
    class CambioDeEstado {

        private String cuerpo(String estado, String justificacion) throws Exception {
            CambioEstadoRequestDTO dto = new CambioEstadoRequestDTO();
            dto.setEstado(estado);
            dto.setNombreTransicion("transicion");
            dto.setJustificacion(justificacion);
            return objectMapper.writeValueAsString(dto);
        }

        @Test
        @DisplayName("Devuelve 204 cuando el cambio es válido")
        void cambioOk() throws Exception {
            mockMvc.perform(patch("/donaciones/{id}/estado", idDonacion)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo("ASIGNACION_REALIZADA", null)))
                    .andExpect(status().isNoContent());

            verify(donacionService).cambiarEstado(eq(idDonacion), any());
        }

        @Test
        @DisplayName("Devuelve 422 si la transición no está permitida")
        void transicionIlegal() throws Exception {
            // 422 (Unprocessable Entity) distingue el "entiendo el pedido pero
            // el estado actual no lo permite" de un simple 400 por datos mal formados.
            doThrow(new CambioEstadoDonacionIlegalException("EN_DEPOSITO", "ENTREGADA"))
                    .when(donacionService).cambiarEstado(eq(idDonacion), any());

            mockMvc.perform(patch("/donaciones/{id}/estado", idDonacion)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo("ENTREGADA", null)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.status").value(422));
        }

        @Test
        @DisplayName("Devuelve 400 si falta la justificación obligatoria")
        void faltaJustificacion() throws Exception {
            doThrow(new FaltaJustificacionDonacionException())
                    .when(donacionService).cambiarEstado(eq(idDonacion), any());

            mockMvc.perform(patch("/donaciones/{id}/estado", idDonacion)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo("ENTREGA_FALLIDA", null)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Devuelve 404 si la donación no existe")
        void donacionInexistente() throws Exception {
            doThrow(new DonacionNoEncontradaException(idDonacion))
                    .when(donacionService).cambiarEstado(eq(idDonacion), any());

            mockMvc.perform(patch("/donaciones/{id}/estado", idDonacion)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo("ASIGNACION_REALIZADA", null)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Devuelve 400 si el estado destino viene en blanco")
        void estadoEnBlanco() throws Exception {
            mockMvc.perform(patch("/donaciones/{id}/estado", idDonacion)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo("", null)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PATCH /donaciones/{id}/asignar - asignación")
    class Asignacion {

        private String cuerpo(UUID idEntidad) throws Exception {
            AsignacionRequestDTO dto = new AsignacionRequestDTO();
            dto.setIdEntidadBeneficiaria(idEntidad);
            return objectMapper.writeValueAsString(dto);
        }

        @Test
        @DisplayName("Devuelve 204 cuando la asignación se concreta")
        void asignacionOk() throws Exception {
            mockMvc.perform(patch("/donaciones/{id}/asignar", idDonacion)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo(UUID.randomUUID())))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Devuelve 404 si la entidad beneficiaria no existe")
        void entidadInexistente() throws Exception {
            UUID idEntidad = UUID.randomUUID();
            doThrow(new EntidadBeneficiariaNoEncontradaException(idEntidad))
                    .when(donacionService).asignar(eq(idDonacion), any());

            mockMvc.perform(patch("/donaciones/{id}/asignar", idDonacion)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo(idEntidad)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Devuelve 400 si no se indica la entidad destino")
        void faltaLaEntidad() throws Exception {
            mockMvc.perform(patch("/donaciones/{id}/asignar", idDonacion)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of())))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PATCH /donaciones/{id}/bien - modificación del bien")
    class ModificacionDeBien {

        private String cuerpo(String subcategoria, int cantidad) throws Exception {
            BienRequestDTO dto = new BienRequestDTO();
            dto.setSubcategoria(subcategoria);
            dto.setDescripcion("Bien corregido");
            dto.setCantidad(cantidad);
            dto.setUnidad("kg");
            return objectMapper.writeValueAsString(dto);
        }

        @Test
        @DisplayName("Devuelve 204 cuando el bien se reemplaza")
        void modificacionOk() throws Exception {
            mockMvc.perform(patch("/donaciones/{id}/bien", idDonacion)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo("arroz", 10)))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Devuelve 409 si la donación no tiene bienes que modificar")
        void sinBienes409() throws Exception {
            doThrow(new DonacionSinBienesException(idDonacion))
                    .when(donacionService).modificarBien(eq(idDonacion), any());

            mockMvc.perform(patch("/donaciones/{id}/bien", idDonacion)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo("arroz", 10)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Devuelve 400 si la cantidad no es positiva")
        void cantidadInvalida() throws Exception {
            mockMvc.perform(patch("/donaciones/{id}/bien", idDonacion)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo("arroz", 0)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /donaciones/{id}/candidatos y DELETE /donaciones/{id}")
    class CandidatosYBaja {

        @Test
        @DisplayName("GET candidatos devuelve 200 con las tres listas del matchmaking")
        void candidatosOk() throws Exception {
            when(donacionService.obtenerCandidatos(idDonacion)).thenReturn(
                    ar.utn.donatrack.donaciones.dtos.response.CandidatosAsignacionResponseDTO.builder()
                            .idDonacion(idDonacion)
                            .porCompatibilidad(List.of())
                            .porSubatendidos(List.of())
                            .coincidencias(List.of())
                            .build());

            mockMvc.perform(get("/donaciones/{id}/candidatos", idDonacion))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.idDonacion").value(idDonacion.toString()))
                    .andExpect(jsonPath("$.porCompatibilidad").isArray())
                    .andExpect(jsonPath("$.porSubatendidos").isArray())
                    .andExpect(jsonPath("$.coincidencias").isArray());
        }

        @Test
        @DisplayName("GET candidatos devuelve 404 si la donación no existe")
        void candidatos404() throws Exception {
            when(donacionService.obtenerCandidatos(idDonacion))
                    .thenThrow(new DonacionNoEncontradaException(idDonacion));

            mockMvc.perform(get("/donaciones/{id}/candidatos", idDonacion))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("DELETE devuelve 204 cuando la donación se elimina")
        void bajaOk() throws Exception {
            mockMvc.perform(delete("/donaciones/{id}", idDonacion))
                    .andExpect(status().isNoContent());

            verify(donacionService).eliminar(idDonacion);
        }

        @Test
        @DisplayName("DELETE devuelve 404 si la donación no existe")
        void baja404() throws Exception {
            doThrow(new DonacionNoEncontradaException(idDonacion)).when(donacionService).eliminar(idDonacion);

            mockMvc.perform(delete("/donaciones/{id}", idDonacion))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /donaciones/candidatos - bandeja del matchmaking nocturno")
    class CandidatosPendientes {

        @Test
        @DisplayName("Devuelve 200 con las donaciones pendientes de confirmar destino")
        void bandejaOk() throws Exception {
            when(donacionService.obtenerCandidatosPendientes()).thenReturn(List.of(
                    CandidatosPendientesResponseDTO.builder()
                            .idDonacion(idDonacion)
                            .fechaCalculo(LocalDateTime.of(2026, 3, 15, 3, 30))
                            .huboCoincidencias(true)
                            .coincidencias(List.of())
                            .porCompatibilidad(List.of())
                            .porSubatendidos(List.of())
                            .build()));

            mockMvc.perform(get("/donaciones/candidatos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                    .andExpect(jsonPath("$[0].idDonacion").value(idDonacion.toString()))
                    .andExpect(jsonPath("$[0].huboCoincidencias").value(true))
                    .andExpect(jsonPath("$[0].fechaCalculo").exists());
        }

        @Test
        @DisplayName("Devuelve 200 con lista vacía si el proceso nocturno todavía no corrió")
        void bandejaVacia() throws Exception {
            when(donacionService.obtenerCandidatosPendientes()).thenReturn(List.of());

            mockMvc.perform(get("/donaciones/candidatos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
        }

        @Test
        @DisplayName("La ruta /candidatos NO colisiona con /donaciones/{id}")
        void noColisionaConElDetalle() throws Exception {
            // Riesgo real: "candidatos" podría interpretarse como un {id} mal
            // formado y caer en obtenerDonacion(). Spring prioriza el patrón
            // literal sobre el de plantilla, y este test lo deja fijado: si
            // alguien reordena los mappings, falla acá.
            when(donacionService.obtenerCandidatosPendientes()).thenReturn(List.of());

            mockMvc.perform(get("/donaciones/candidatos"))
                    .andExpect(status().isOk());

            verify(donacionService).obtenerCandidatosPendientes();
            verify(donacionService, never()).obtenerPorId(any());
        }
    }
}
