package ar.utn.donatrack.donaciones.controllers;

import ar.utn.donatrack.donaciones.dtos.response.ImportacionResumenResponseDTO;
import ar.utn.donatrack.donaciones.util.SolicitudPagina;
import ar.utn.donatrack.donaciones.util.Pagina;
import ar.utn.donatrack.donaciones.dtos.response.PaginaResponseDTO;
import ar.utn.donatrack.donaciones.dtos.request.DireccionRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.EmailRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.LocalidadRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.PersonaHumanaRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.ProvinciaRequestDTO;
import ar.utn.donatrack.donaciones.dtos.response.PersonaHumanaResponseDTO;
import ar.utn.donatrack.donaciones.exceptions.mediosContactoExceptions.EmailInvalidoException;
import ar.utn.donatrack.donaciones.exceptions.mediosContactoExceptions.EmailYaRegistradoException;
import ar.utn.donatrack.donaciones.exceptions.cambioEstadosExceptions.TransicionDeEstadoIlegalException;
import ar.utn.donatrack.donaciones.exceptions.cambioEstadosExceptions.FaltaJustificacionException;
import ar.utn.donatrack.donaciones.exceptions.personasExceptions.PersonaConMismoEstadoException;
import ar.utn.donatrack.donaciones.exceptions.personasExceptions.PersonaDonanteNoEncontradaException;
import ar.utn.donatrack.donaciones.exceptions.personasExceptions.TipoPersonaIlegalException;
import ar.utn.donatrack.donaciones.importacion.ImportFilaCSV;
import ar.utn.donatrack.donaciones.importacion.ImportReport;
import ar.utn.donatrack.donaciones.interfaces.services.PersonaDonanteServiceInterface;
import ar.utn.donatrack.donaciones.models.donante.Genero;
import ar.utn.donatrack.donaciones.services.CsvImportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de la capa REST de personas donantes.
 *
 * Como en el resto de los tests de controller, se levanta solo la capa web y
 * los services van mockeados: acá se verifica el contrato HTTP.
 *
 * Puntos particulares de este controller:
 *   - El POST devuelve 201 CON cuerpo y cabecera Location (requisito acordado
 *     para que el cliente conozca el id generado sin hacer un GET extra).
 *   - El polimorfismo del JSON: el campo "tipo" (HUMANA / JURIDICA) decide qué
 *     subtipo de DTO deserializa Jackson.
 *   - La importación masiva se expone como multipart/form-data.
 */
@WebMvcTest(PersonaDonanteController.class)
@DisplayName("PersonaDonanteController - contrato HTTP de /donantes")
class PersonaDonanteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PersonaDonanteServiceInterface personaDonanteService;

    @MockitoBean
    private CsvImportService csvImportService;

    private final UUID idDonante = UUID.randomUUID();

    private PersonaHumanaResponseDTO respuestaHumana() {
        return PersonaHumanaResponseDTO.builder()
                .id(idDonante)
                .nombre("Juan")
                .apellido("Pérez")
                .edad(35)
                .tipoDocumento("DNI")
                .numeroDocumento("30111222")
                .email("juan@example.com")
                .estado("ACTIVO")
                .build();
    }

    private DireccionRequestDTO direccion() {
        return DireccionRequestDTO.builder()
                .calle("Medrano")
                .numero(951)
                .codigoPostal("C1179")
                .localidad(LocalidadRequestDTO.builder()
                        .nombre("CABA")
                        .provincia(ProvinciaRequestDTO.builder().nombre("Buenos Aires").build())
                        .build())
                .build();
    }

    /**
     * Cuerpo de alta de una persona humana. Se arma como Map para poder incluir
     * el discriminador "tipo", que Jackson necesita para elegir el subtipo y que
     * no forma parte de los campos del DTO.
     */
    private String cuerpoAltaHumana(String email) throws Exception {
        PersonaHumanaRequestDTO dto = PersonaHumanaRequestDTO.builder()
                .nombre("Juan")
                .apellido("Pérez")
                .fechaNacimiento(LocalDate.of(1990, 5, 20))
                .genero(Genero.MASCULINO)
                .tipoDocumento("DNI")
                .numeroDocumento("30111222")
                .email(email)
                .direccion(direccion())
                .medioContactoPredeterminado(EmailRequestDTO.builder().valor(email).build())
                .build();
        return objectMapper.writeValueAsString(dto);
    }

    @Nested
    @DisplayName("GET /donantes - listado y detalle")
    class Consulta {

        @Test
        @DisplayName("Devuelve 200 con la página de donantes y sus totales")
        void listaDonantes() throws Exception {
            when(personaDonanteService.obtenerDonantes(isNull(), any()))
                    .thenReturn(PaginaResponseDTO.desde(new Pagina<>(List.of(respuestaHumana()), 0, 20, 1)));

            mockMvc.perform(get("/donantes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.contenido", org.hamcrest.Matchers.hasSize(1)))
                    .andExpect(jsonPath("$.contenido[0].estado").value("ACTIVO"))
                    .andExpect(jsonPath("$.totalElementos").value(1));

            verify(personaDonanteService).obtenerDonantes(null, new SolicitudPagina(0, 20));
        }

        @Test
        @DisplayName("Pasa el filtro de estado y la página al service")
        void filtraPorEstado() throws Exception {
            when(personaDonanteService.obtenerDonantes(any(), any()))
                    .thenReturn(PaginaResponseDTO.desde(new Pagina<>(List.of(), 1, 10, 0)));

            mockMvc.perform(get("/donantes").param("estado", "BLOQUEADO").param("page", "1").param("size", "10"))
                    .andExpect(status().isOk());

            verify(personaDonanteService).obtenerDonantes("BLOQUEADO", new SolicitudPagina(1, 10));
        }

        @Test
        @DisplayName("Devuelve 400 si el tamaño de página es 0")
        void tamanioCero() throws Exception {
            mockMvc.perform(get("/donantes").param("size", "0"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Devuelve 200 con el detalle del donante")
        void detalleOk() throws Exception {
            when(personaDonanteService.obtenerDonante(idDonante)).thenReturn(respuestaHumana());

            mockMvc.perform(get("/donantes/{id}", idDonante))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(idDonante.toString()))
                    .andExpect(jsonPath("$.email").value("juan@example.com"));
        }

        @Test
        @DisplayName("Devuelve 404 si el donante no existe")
        void detalle404() throws Exception {
            when(personaDonanteService.obtenerDonante(idDonante))
                    .thenThrow(new PersonaDonanteNoEncontradaException(idDonante));

            mockMvc.perform(get("/donantes/{id}", idDonante))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    @Nested
    @DisplayName("POST /donantes - alta")
    class Alta {

        @Test
        @DisplayName("Devuelve 201 con el donante creado y la cabecera Location")
        void altaOk() throws Exception {
            // El 201 con cuerpo evita que el cliente tenga que hacer un GET extra
            // solo para averiguar el id que le asignó el sistema.
            when(personaDonanteService.registrar(any())).thenReturn(respuestaHumana());

            mockMvc.perform(post("/donantes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoAltaHumana("juan@example.com")))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/donantes/" + idDonante))
                    .andExpect(jsonPath("$.id").value(idDonante.toString()))
                    .andExpect(jsonPath("$.estado").value("ACTIVO"));
        }

        @Test
        @DisplayName("Devuelve 400 si el email tiene formato inválido")
        void emailInvalido() throws Exception {
            when(personaDonanteService.registrar(any())).thenThrow(new EmailInvalidoException("mal"));

            mockMvc.perform(post("/donantes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoAltaHumana("juan@example.com")))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Devuelve 409 si el email ya está registrado")
        void emailDuplicado() throws Exception {
            // 409 Conflict: el pedido es válido pero choca con el estado actual
            // del sistema (ya existe un donante con ese email).
            when(personaDonanteService.registrar(any()))
                    .thenThrow(new EmailYaRegistradoException("juan@example.com"));

            mockMvc.perform(post("/donantes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoAltaHumana("juan@example.com")))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Devuelve 409 si el UNIQUE de la base rechaza el email (alta simultánea)")
        void emailDuplicadoEnLaBase() throws Exception {
            // Dos altas simultáneas pasan la validación previa: la segunda la
            // frena el UNIQUE de la columna, con el estado SQL de clave duplicada.
            when(personaDonanteService.registrar(any()))
                    .thenThrow(new DataIntegrityViolationException("duplicado",
                            new SQLException("unique violation", "23505")));

            mockMvc.perform(post("/donantes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoAltaHumana("juan@example.com")))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Otra violación de integridad (no de unicidad) sigue siendo un 500")
        void otraViolacionDeIntegridad() throws Exception {
            when(personaDonanteService.registrar(any()))
                    .thenThrow(new DataIntegrityViolationException("not null",
                            new SQLException("null value", "23502")));

            mockMvc.perform(post("/donantes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoAltaHumana("juan@example.com")))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("Devuelve 400 si el JSON no indica el tipo de persona")
        void faltaElDiscriminador() throws Exception {
            // Sin "tipo" Jackson no sabe si construir una persona humana o
            // jurídica, y el cuerpo se rechaza como ilegible.
            mockMvc.perform(post("/donantes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "nombre", "Juan",
                                    "email", "juan@example.com"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Devuelve 400 si faltan campos obligatorios del donante")
        void faltanCamposObligatorios() throws Exception {
            mockMvc.perform(post("/donantes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("tipo", "HUMANA"))))
                    .andExpect(status().isBadRequest());

            verify(personaDonanteService, never()).registrar(any());
        }
    }

    @Nested
    @DisplayName("PUT /donantes/{id} - actualización")
    class Actualizacion {

        @Test
        @DisplayName("Devuelve 200 con el donante actualizado")
        void actualizacionOk() throws Exception {
            when(personaDonanteService.actualizar(eq(idDonante), any())).thenReturn(respuestaHumana());

            mockMvc.perform(put("/donantes/{id}", idDonante)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("nombre", "Juan Carlos"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(idDonante.toString()));
        }

        @Test
        @DisplayName("Acepta un cuerpo con solo algunos campos (actualización parcial)")
        void actualizacionParcial() throws Exception {
            // Ningún campo del DTO de actualización es obligatorio: el service
            // aplica solo lo que llega.
            when(personaDonanteService.actualizar(eq(idDonante), any())).thenReturn(respuestaHumana());

            mockMvc.perform(put("/donantes/{id}", idDonante)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Devuelve 404 si el donante no existe")
        void actualizar404() throws Exception {
            when(personaDonanteService.actualizar(eq(idDonante), any()))
                    .thenThrow(new PersonaDonanteNoEncontradaException(idDonante));

            mockMvc.perform(put("/donantes/{id}", idDonante)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /donantes/{id}/estado - cambio de estado")
    class CambioDeEstado {

        private String cuerpo(String estado, String justificacion) throws Exception {
            return objectMapper.writeValueAsString(justificacion == null
                    ? Map.of("estado", estado)
                    : Map.of("estado", estado, "justificacion", justificacion));
        }

        @Test
        @DisplayName("Devuelve 204 cuando el cambio es válido")
        void cambioOk() throws Exception {
            mockMvc.perform(patch("/donantes/{id}/estado", idDonante)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo("INACTIVO", null)))
                    .andExpect(status().isNoContent());

            verify(personaDonanteService).cambiarEstado(eq(idDonante), any());
        }

        @Test
        @DisplayName("Devuelve 422 si la transición no está permitida")
        void transicionIlegal() throws Exception {
            doThrow(new TransicionDeEstadoIlegalException("INACTIVO", "BLOQUEADO"))
                    .when(personaDonanteService).cambiarEstado(eq(idDonante), any());

            mockMvc.perform(patch("/donantes/{id}/estado", idDonante)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo("BLOQUEADO", "motivo")))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("Devuelve 409 si el donante ya está en ese estado")
        void mismoEstado() throws Exception {
            doThrow(new PersonaConMismoEstadoException("ACTIVO"))
                    .when(personaDonanteService).cambiarEstado(eq(idDonante), any());

            mockMvc.perform(patch("/donantes/{id}/estado", idDonante)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo("ACTIVO", null)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Devuelve 400 si se bloquea sin justificación")
        void faltaJustificacion() throws Exception {
            doThrow(new FaltaJustificacionException("Se requiere justificación para bloquear"))
                    .when(personaDonanteService).cambiarEstado(eq(idDonante), any());

            mockMvc.perform(patch("/donantes/{id}/estado", idDonante)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo("BLOQUEADO", null)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Devuelve 400 si el estado destino viene en blanco")
        void estadoEnBlanco() throws Exception {
            mockMvc.perform(patch("/donantes/{id}/estado", idDonante)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("estado", ""))))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PATCH de contactos y representantes")
    class ContactosYRepresentantes {

        @Test
        @DisplayName("Modificar el contacto devuelve 204")
        void contactoOk() throws Exception {
            mockMvc.perform(patch("/donantes/{id}/contactos", idDonante)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "tipo", "EMAIL", "valor", "nuevo@example.com"))))
                    .andExpect(status().isNoContent());

            verify(personaDonanteService).modificarContacto(eq(idDonante), any());
        }

        @Test
        @DisplayName("Devuelve 400 si el contacto no trae valor")
        void contactoSinValor() throws Exception {
            mockMvc.perform(patch("/donantes/{id}/contactos", idDonante)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("tipo", "EMAIL", "valor", ""))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Modificar un representante devuelve 204")
        void representanteOk() throws Exception {
            mockMvc.perform(patch("/donantes/{id}/representantes", idDonante)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "nombre", "Ana", "apellido", "Gómez", "email", "ana@fundacion.org"))))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Devuelve 400 al pedir representantes sobre una persona humana")
        void representanteEnPersonaHumana() throws Exception {
            doThrow(new TipoPersonaIlegalException(idDonante))
                    .when(personaDonanteService).modificarRepresentante(eq(idDonante), any());

            mockMvc.perform(patch("/donantes/{id}/representantes", idDonante)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "nombre", "Ana", "apellido", "Gómez", "email", "ana@fundacion.org"))))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /donantes/{id} - baja")
    class Baja {

        @Test
        @DisplayName("Devuelve 204 cuando el donante se elimina")
        void bajaOk() throws Exception {
            mockMvc.perform(delete("/donantes/{id}", idDonante))
                    .andExpect(status().isNoContent());

            verify(personaDonanteService).eliminar(idDonante);
        }

        @Test
        @DisplayName("Devuelve 404 si el donante no existe")
        void baja404() throws Exception {
            doThrow(new PersonaDonanteNoEncontradaException(idDonante))
                    .when(personaDonanteService).eliminar(idDonante);

            mockMvc.perform(delete("/donantes/{id}", idDonante))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /donantes/import - importación masiva por CSV")
    class Importacion {

        @Test
        @DisplayName("Devuelve 200 con el resumen de la importación, sin una entrada por fila")
        void importacionOk() throws Exception {
            ImportReport reporte = new ImportReport();
            reporte.agregar(ImportFilaCSV.creado(2, "juan@example.com"));
            reporte.agregar(ImportFilaCSV.actualizado(3, "ana@example.com"));
            when(csvImportService.importarYResumir(any())).thenReturn(ImportacionResumenResponseDTO.desde(reporte, 100));

            MockMultipartFile archivo = new MockMultipartFile(
                    "file", "donantes.csv", "text/csv",
                    "TipoPersona,TipoDoc,Documento,Nombre,Email\nHUMANA,DNI,30111222,Juan Pérez,juan@example.com\n"
                            .getBytes(StandardCharsets.UTF_8));

            mockMvc.perform(multipart("/donantes/import").file(archivo))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalProcesadas").value(2))
                    .andExpect(jsonPath("$.creadas").value(1))
                    .andExpect(jsonPath("$.actualizadas").value(1))
                    .andExpect(jsonPath("$.conError").value(0))
                    .andExpect(jsonPath("$.errores", org.hamcrest.Matchers.hasSize(0)))
                    .andExpect(jsonPath("$.resultados").doesNotExist());
        }

        @Test
        @DisplayName("El reporte informa los errores por línea sin fallar la petición")
        void reporteConErrores() throws Exception {
            // La importación devuelve 200 aunque haya filas con error: el archivo
            // se procesó, y el detalle de qué corregir va en el cuerpo.
            ImportReport reporte = new ImportReport();
            reporte.agregar(ImportFilaCSV.creado(2, "juan@example.com"));
            reporte.agregar(ImportFilaCSV.error(3, "", "Tipo de persona inválido"));
            when(csvImportService.importarYResumir(any())).thenReturn(ImportacionResumenResponseDTO.desde(reporte, 100));

            MockMultipartFile archivo = new MockMultipartFile(
                    "file", "donantes.csv", "text/csv", "contenido".getBytes(StandardCharsets.UTF_8));

            mockMvc.perform(multipart("/donantes/import").file(archivo))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.conError").value(1))
                    .andExpect(jsonPath("$.errores", org.hamcrest.Matchers.hasSize(1)))
                    .andExpect(jsonPath("$.errores[0].numeroLinea").value(3))
                    .andExpect(jsonPath("$.errores[0].detalle").value("Tipo de persona inválido"))
                    .andExpect(jsonPath("$.erroresOmitidos").value(0));
        }
    }
}
