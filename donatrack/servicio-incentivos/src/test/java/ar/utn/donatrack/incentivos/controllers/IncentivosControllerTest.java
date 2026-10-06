package ar.utn.donatrack.incentivos.controllers;

import ar.utn.donatrack.incentivos.exceptions.CategoriasDonadasInvalidasException;
import ar.utn.donatrack.incentivos.exceptions.DonanteNoEncontradoException;
import ar.utn.donatrack.incentivos.exceptions.MisionNoEncontradaException;
import ar.utn.donatrack.incentivos.interfaces.services.IncentivosServiceInterface;
import ar.utn.donatrack.incentivos.models.Donante;
import ar.utn.donatrack.incentivos.models.categoriasdonante.Colaborador;
import ar.utn.donatrack.incentivos.models.insignias.Insignia;
import ar.utn.donatrack.incentivos.models.insignias.InsigniaObtenida;
import ar.utn.donatrack.incentivos.models.misiones.DonacionesExitosas;
import ar.utn.donatrack.incentivos.models.misiones.Mision;
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

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de la capa REST de incentivos.
 *
 * Se verifica por endpoint:
 * - ruta, verbo y codigo de estado esperado;
 * - que el body recibido se transforme bien antes de llamar al service;
 * - que los POST devuelvan texto y no una respuesta vacia;
 * - que las excepciones del dominio se traduzcan al status HTTP correcto.
 */
@WebMvcTest(IncentivosController.class)
@DisplayName("IncentivosController - contrato HTTP de /api/incentivos/donantes")
class IncentivosControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IncentivosServiceInterface service;

    @Nested
    @DisplayName("GET /{id}/metricas")
    class Metricas {

        @Test
        @DisplayName("Devuelve 200 con el perfil calculado")
        void metricasOk() throws Exception {
            UUID donanteId = UUID.randomUUID();
            Donante donante = donanteColaborador(donanteId);
            when(service.obtenerPerfil(donanteId)).thenReturn(donante);
            when(service.obtenerPosicionRankingActual(donanteId)).thenReturn(2);

            mockMvc.perform(get("/api/incentivos/donantes/{id}/metricas", donanteId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.donanteId").value(donanteId.toString()))
                    .andExpect(jsonPath("$.categoriaActual").value("Colaborador"))
                    .andExpect(jsonPath("$.totalDonacionesHistoricas").value(0))
                    .andExpect(jsonPath("$.donacionesMesActual").value(0))
                    .andExpect(jsonPath("$.organizacionesAyudadas").value(0))
                    .andExpect(jsonPath("$.posicionRanking").value(2));
        }

        @Test
        @DisplayName("Devuelve 404 si el donante no existe")
        void donanteInexistente() throws Exception {
            UUID donanteId = UUID.randomUUID();
            when(service.obtenerPerfil(donanteId)).thenThrow(new DonanteNoEncontradoException(donanteId));

            mockMvc.perform(get("/api/incentivos/donantes/{id}/metricas", donanteId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("Devuelve 400 si el id no es UUID")
        void idInvalido() throws Exception {
            mockMvc.perform(get("/api/incentivos/donantes/{id}/metricas", "no-es-uuid"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /{id}/misiones")
    class Misiones {

        @Test
        @DisplayName("Devuelve 200 con las misiones y su progreso")
        void misionesOk() throws Exception {
            UUID donanteId = UUID.randomUUID();
            Donante donante = donanteColaborador(donanteId);
            Mision mision = new DonacionesExitosas("Primera donacion", "Completar entrega", new Colaborador(), 1, insignia());
            when(service.obtenerPerfil(donanteId)).thenReturn(donante);
            when(service.obtenerMisiones(donanteId)).thenReturn(List.of(mision));

            mockMvc.perform(get("/api/incentivos/donantes/{id}/misiones", donanteId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].nombre").value("Primera donacion"))
                    .andExpect(jsonPath("$[0].tipo").value("DonacionesExitosas"))
                    .andExpect(jsonPath("$[0].categoriaRequerida").value("Colaborador"))
                    .andExpect(jsonPath("$[0].objetivo").value(1))
                    .andExpect(jsonPath("$[0].progresoActual").value(0))
                    .andExpect(jsonPath("$[0].distanciaRestante").value(1))
                    .andExpect(jsonPath("$[0].completada").value(false));
        }

        @Test
        @DisplayName("Devuelve 200 con lista vacia si no hay misiones para mostrar")
        void listaVacia() throws Exception {
            UUID donanteId = UUID.randomUUID();
            when(service.obtenerPerfil(donanteId)).thenReturn(donanteColaborador(donanteId));
            when(service.obtenerMisiones(donanteId)).thenReturn(List.of());

            mockMvc.perform(get("/api/incentivos/donantes/{id}/misiones", donanteId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("Devuelve 404 si la categoria no tiene misiones")
        void misionesNoEncontradas() throws Exception {
            UUID donanteId = UUID.randomUUID();
            when(service.obtenerPerfil(donanteId)).thenReturn(donanteColaborador(donanteId));
            when(service.obtenerMisiones(donanteId)).thenThrow(new MisionNoEncontradaException("Colaborador"));

            mockMvc.perform(get("/api/incentivos/donantes/{id}/misiones", donanteId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    @Nested
    @DisplayName("GET /{id}/insignias")
    class Insignias {

        @Test
        @DisplayName("Devuelve 200 con las insignias obtenidas")
        void insigniasOk() throws Exception {
            UUID donanteId = UUID.randomUUID();
            when(service.obtenerInsignias(donanteId)).thenReturn(List.of(new InsigniaObtenida(insignia(), true)));

            mockMvc.perform(get("/api/incentivos/donantes/{id}/insignias", donanteId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].nombre").value("Semilla"))
                    .andExpect(jsonPath("$[0].imagen").value("semilla.png"))
                    .andExpect(jsonPath("$[0].visible").value(true));
        }

        @Test
        @DisplayName("Devuelve 200 con lista vacia si aun no gano insignias")
        void listaVacia() throws Exception {
            UUID donanteId = UUID.randomUUID();
            when(service.obtenerInsignias(donanteId)).thenReturn(List.of());

            mockMvc.perform(get("/api/incentivos/donantes/{id}/insignias", donanteId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("POST /donacion")
    class DonacionRegistrada {

        @Test
        @DisplayName("Devuelve 201 con texto cuando registra la donacion")
        void donacionOk() throws Exception {
            UUID donanteId = UUID.randomUUID();

            mockMvc.perform(post("/api/incentivos/donantes/donacion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bodyDonacion(donanteId))))
                    .andExpect(status().isCreated())
                    .andExpect(content().string("Donacion registrada en incentivos para el donante " + donanteId));

            verify(service).procesarDonacion(eq(donanteId), any(), eq("donante@mail.com"), eq("EMAIL"));
        }

        @Test
        @DisplayName("Pasa cantidad, categorias y entidad al service")
        void pasaDatosAlService() throws Exception {
            UUID donanteId = UUID.randomUUID();

            mockMvc.perform(post("/api/incentivos/donantes/donacion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bodyDonacion(donanteId))))
                    .andExpect(status().isCreated());

            verify(service).procesarDonacion(eq(donanteId), argThat(donacion ->
                    donacion.getCantidadBienes() == 5
                            && donacion.getCategorias().contains("ALIMENTOS")
                            && donacion.getCategorias().contains("ABRIGO")
                            && !donacion.isExitosa()
                            && "Comedor Norte".equals(donacion.getEntidadBeneficiaria())
            ), eq("donante@mail.com"), eq("EMAIL"));
        }

        @Test
        @DisplayName("Devuelve 400 si falta el donante")
        void faltaDonante() throws Exception {
            Map<String, Object> body = Map.of(
                    "destinatario", "donante@mail.com",
                    "medio", "EMAIL",
                    "cantidadBienes", 5,
                    "categoriasDonadas", List.of("ALIMENTOS")
            );

            mockMvc.perform(post("/api/incentivos/donantes/donacion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());

            verify(service, never()).procesarDonacion(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Devuelve 400 si faltan las categorias")
        void faltanCategorias() throws Exception {
            UUID donanteId = UUID.randomUUID();
            Map<String, Object> body = Map.of(
                    "donanteId", donanteId.toString(),
                    "destinatario", "donante@mail.com",
                    "medio", "EMAIL",
                    "cantidadBienes", 5,
                    "categoriasDonadas", List.of()
            );

            mockMvc.perform(post("/api/incentivos/donantes/donacion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());

            verify(service, never()).procesarDonacion(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Devuelve 400 si el medio viene en blanco")
        void medioEnBlanco() throws Exception {
            UUID donanteId = UUID.randomUUID();
            Map<String, Object> body = Map.of(
                    "donanteId", donanteId.toString(),
                    "destinatario", "donante@mail.com",
                    "medio", " ",
                    "cantidadBienes", 5,
                    "categoriasDonadas", List.of("ALIMENTOS")
            );

            mockMvc.perform(post("/api/incentivos/donantes/donacion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());

            verify(service, never()).procesarDonacion(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Devuelve 400 si la cantidad no es positiva")
        void cantidadInvalida() throws Exception {
            UUID donanteId = UUID.randomUUID();
            Map<String, Object> body = Map.of(
                    "donanteId", donanteId.toString(),
                    "destinatario", "donante@mail.com",
                    "medio", "EMAIL",
                    "cantidadBienes", 0,
                    "categoriasDonadas", List.of("ALIMENTOS")
            );

            mockMvc.perform(post("/api/incentivos/donantes/donacion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());

            verify(service, never()).procesarDonacion(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Devuelve 400 si el JSON esta mal formado")
        void jsonInvalido() throws Exception {
            mockMvc.perform(post("/api/incentivos/donantes/donacion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ esto no es json valido"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Devuelve 400 si el service rechaza las categorias")
        void categoriasInvalidas() throws Exception {
            UUID donanteId = UUID.randomUUID();
            doThrow(new CategoriasDonadasInvalidasException())
                    .when(service).procesarDonacion(eq(donanteId), any(), eq("donante@mail.com"), eq("EMAIL"));

            mockMvc.perform(post("/api/incentivos/donantes/donacion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bodyDonacion(donanteId))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }
    }

    @Nested
    @DisplayName("POST /donacion-exitosa")
    class DonacionExitosa {

        @Test
        @DisplayName("Devuelve 201 con texto cuando registra la entrega")
        void donacionExitosaOk() throws Exception {
            UUID donanteId = UUID.randomUUID();

            mockMvc.perform(post("/api/incentivos/donantes/donacion-exitosa")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bodyDonacion(donanteId))))
                    .andExpect(status().isCreated())
                    .andExpect(content().string("Donacion exitosa registrada en incentivos para el donante " + donanteId));

            verify(service).procesarDonacionExitosa(eq(donanteId), any(), eq("donante@mail.com"), eq("EMAIL"));
        }

        @Test
        @DisplayName("Pasa la donacion como exitosa al service")
        void pasaDonacionExitosaAlService() throws Exception {
            UUID donanteId = UUID.randomUUID();

            mockMvc.perform(post("/api/incentivos/donantes/donacion-exitosa")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bodyDonacion(donanteId))))
                    .andExpect(status().isCreated());

            verify(service).procesarDonacionExitosa(eq(donanteId), argThat(donacion ->
                    donacion.isExitosa()
                            && donacion.getCantidadBienes() == 5
                            && donacion.getCategorias().contains("ALIMENTOS")
                            && "Comedor Norte".equals(donacion.getEntidadBeneficiaria())
            ), eq("donante@mail.com"), eq("EMAIL"));
        }

        @Test
        @DisplayName("Devuelve 400 si falta el donante")
        void faltaDonante() throws Exception {
            Map<String, Object> body = Map.of(
                    "destinatario", "donante@mail.com",
                    "medio", "EMAIL",
                    "cantidadBienes", 5,
                    "categoriasDonadas", List.of("ALIMENTOS")
            );

            mockMvc.perform(post("/api/incentivos/donantes/donacion-exitosa")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());

            verify(service, never()).procesarDonacionExitosa(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Devuelve 400 si falta el medio")
        void faltaMedio() throws Exception {
            UUID donanteId = UUID.randomUUID();
            Map<String, Object> body = Map.of(
                    "donanteId", donanteId.toString(),
                    "destinatario", "donante@mail.com",
                    "medio", " ",
                    "cantidadBienes", 5,
                    "categoriasDonadas", List.of("ALIMENTOS")
            );

            mockMvc.perform(post("/api/incentivos/donantes/donacion-exitosa")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());

            verify(service, never()).procesarDonacionExitosa(any(), any(), any(), any());
        }
    }

    private Map<String, Object> bodyDonacion(UUID donanteId) {
        return Map.of(
                "donanteId", donanteId.toString(),
                "destinatario", "donante@mail.com",
                "medio", "EMAIL",
                "cantidadBienes", 5,
                "categoriasDonadas", List.of("ALIMENTOS", "ABRIGO"),
                "fecha", "2026-06-24T19:07:54",
                "entidadBeneficiaria", "Comedor Norte"
        );
    }

    private Donante donanteColaborador(UUID id) {
        Donante donante = new Donante();
        donante.setId(id);
        donante.setCategoria(new Colaborador());
        return donante;
    }

    private Insignia insignia() {
        return Insignia.builder().id(UUID.randomUUID()).nombre("Semilla").imagen("semilla.png").build();
    }
}
