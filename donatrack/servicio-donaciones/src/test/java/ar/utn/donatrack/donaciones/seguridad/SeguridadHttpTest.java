package ar.utn.donatrack.donaciones.seguridad;

import ar.utn.donatrack.donaciones.config.SeguridadConfig;
import ar.utn.donatrack.donaciones.controllers.DonacionesController;
import ar.utn.donatrack.donaciones.controllers.EntidadBeneficiariaController;
import ar.utn.donatrack.donaciones.controllers.LogisticaEventosController;
import ar.utn.donatrack.donaciones.controllers.PersonaDonanteController;
import ar.utn.donatrack.donaciones.controllers.PublicoController;
import ar.utn.donatrack.donaciones.dtos.request.BienRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.DireccionRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.DonacionRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.EmailRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.LocalidadRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.PersonaHumanaRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.ProvinciaRequestDTO;
import ar.utn.donatrack.donaciones.dtos.response.DonacionResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.EntidadBeneficiariaResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.PaginaResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.PersonaHumanaResponseDTO;
import ar.utn.donatrack.donaciones.models.donante.Genero;
import ar.utn.donatrack.donaciones.services.ConsultasPublicasService;
import ar.utn.donatrack.donaciones.services.CsvImportService;
import ar.utn.donatrack.donaciones.services.DonacionComandosService;
import ar.utn.donatrack.donaciones.services.DonacionConsultasService;
import ar.utn.donatrack.donaciones.services.EntidadesBeneficiariasService;
import ar.utn.donatrack.donaciones.services.PersonaDonanteService;
import ar.utn.donatrack.donaciones.services.PlanificacionEntregasService;
import ar.utn.donatrack.donaciones.services.SegmentadorDonacionesService;
import ar.utn.donatrack.donaciones.services.logistica.ManejadorEntregaExitosa;
import ar.utn.donatrack.donaciones.services.logistica.ManejadorEntregaFallida;
import ar.utn.donatrack.donaciones.services.logistica.ManejadorInicioDeRuta;
import ar.utn.donatrack.donaciones.util.FiltroDonaciones;
import ar.utn.donatrack.donaciones.util.Pagina;
import ar.utn.donatrack.donaciones.util.SolicitudPagina;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
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
 * Permisos de la API con la seguridad real (Entrega 6): qué puede hacer cada rol
 * en cada ruta y que cada uno opere solo sobre lo suyo.
 *
 * A diferencia de los tests de cada controller, acá no hay token por defecto:
 * cada pedido dice con qué cuenta va (TokensDePrueba). Los services están
 * mockeados: lo que se prueba es si el pedido llega o no a ellos.
 */
@WebMvcTest({DonacionesController.class, PersonaDonanteController.class,
        EntidadBeneficiariaController.class, LogisticaEventosController.class, PublicoController.class})
@Import({SeguridadConfig.class, ControlDeAcceso.class})
@DisplayName("Seguridad: permisos por rol y control de dueño")
class SeguridadHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean private DonacionConsultasService consultas;
    @MockitoBean private DonacionComandosService comandos;
    @MockitoBean private SegmentadorDonacionesService segmentador;
    @MockitoBean private PlanificacionEntregasService planificacion;
    @MockitoBean private PersonaDonanteService donantes;
    @MockitoBean private CsvImportService importacion;
    @MockitoBean private EntidadesBeneficiariasService entidades;
    @MockitoBean private ManejadorInicioDeRuta inicioDeRuta;
    @MockitoBean private ManejadorEntregaExitosa entregaExitosa;
    @MockitoBean private ManejadorEntregaFallida entregaFallida;
    @MockitoBean private ConsultasPublicasService publico;

    private static final String EMAIL_DONANTE = "juan@example.com";
    private static final String EMAIL_ENTIDAD = "comedor@example.org";

    private final UUID idDonante = UUID.randomUUID();
    private final UUID otroDonante = UUID.randomUUID();
    private final UUID idEntidad = UUID.randomUUID();
    private final UUID otraEntidad = UUID.randomUUID();
    private final UUID idDonacion = UUID.randomUUID();

    // ── Datos de ejemplo ────────────────────────────────────────────────────

    private DonacionResponseDTO donacion(UUID donante, UUID entidadAsignada) {
        return DonacionResponseDTO.builder()
                .id(idDonacion)
                .idDonante(donante)
                .idEntidadBeneficiaria(entidadAsignada)
                .descripcion("Colecta")
                .subcategoria("arroz")
                .estado("EN_DEPOSITO")
                .bienes(List.of())
                .historialEstados(List.of())
                .build();
    }

    private PaginaResponseDTO<DonacionResponseDTO> paginaVacia() {
        return PaginaResponseDTO.desde(new Pagina<>(List.<DonacionResponseDTO>of(), 0, 20, 0));
    }

    private String cuerpoDonacion(UUID donante) throws Exception {
        BienRequestDTO bien = new BienRequestDTO();
        bien.setSubcategoria("arroz");
        bien.setDescripcion("Arroz largo fino");
        bien.setCantidad(10);
        bien.setUnidad("kg");
        return objectMapper.writeValueAsString(new DonacionRequestDTO(donante, "Colecta de alimentos", List.of(bien)));
    }

    private DireccionRequestDTO direccion() {
        return DireccionRequestDTO.builder()
                .calle("Medrano").numero(951).codigoPostal("C1179")
                .localidad(LocalidadRequestDTO.builder()
                        .nombre("CABA")
                        .provincia(ProvinciaRequestDTO.builder().nombre("Buenos Aires").build())
                        .build())
                .build();
    }

    private String cuerpoAltaHumana(String email) throws Exception {
        return objectMapper.writeValueAsString(PersonaHumanaRequestDTO.builder()
                .nombre("Juan").apellido("Pérez")
                .fechaNacimiento(LocalDate.of(1990, 5, 20)).genero(Genero.MASCULINO)
                .tipoDocumento("DNI").numeroDocumento("30111222")
                .email(email)
                .direccion(direccion())
                .medioContactoPredeterminado(EmailRequestDTO.builder().valor(email).build())
                .build());
    }

    private String cuerpoAltaEntidad(String emailRepresentante) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "razonSocial", "Comedor Los Pibes",
                "direccion", Map.of("calle", "Medrano", "numero", 951, "codigoPostal", "C1179",
                        "localidad", Map.of("nombre", "CABA", "provincia", Map.of("nombre", "Buenos Aires"))),
                "representantes", List.of(Map.of(
                        "nombre", "Ana", "apellido", "García", "email", emailRepresentante))));
    }

    private PersonaHumanaResponseDTO donanteCreado() {
        return PersonaHumanaResponseDTO.builder().id(idDonante).nombre("Juan").apellido("Pérez")
                .email(EMAIL_DONANTE).estado("ACTIVO").build();
    }

    private EntidadBeneficiariaResponseDTO entidadCreada() {
        return EntidadBeneficiariaResponseDTO.builder().id(idEntidad).razonSocial("Comedor Los Pibes")
                .contactos(List.of()).representantes(List.of()).campanias(List.of()).build();
    }

    // ── Sin token ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Sin token")
    class SinToken {

        @Test
        @DisplayName("Una ruta protegida responde 401 con el ErrorResponseDTO y el encabezado WWW-Authenticate")
        void rutaProtegida() throws Exception {
            mockMvc.perform(get("/donaciones").with(anonymous()))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("Bearer")))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.error").value("Unauthorized"))
                    .andExpect(jsonPath("$.message").value(containsString("token")));

            verify(consultas, never()).obtenerDonaciones(any(), any());
        }

        @Test
        @DisplayName("Los datos públicos de la landing no piden token, pero solo se leen")
        void datosPublicos() throws Exception {
            when(publico.entregadas(any(), any(), any()))
                    .thenReturn(PaginaResponseDTO.desde(new Pagina<>(List.of(), 0, 20, 0)));
            when(publico.subcategorias()).thenReturn(List.of("Arroz"));

            mockMvc.perform(get("/publico/donaciones-entregadas").with(anonymous())).andExpect(status().isOk());
            mockMvc.perform(get("/publico/subcategorias").with(anonymous())).andExpect(status().isOk());
            mockMvc.perform(post("/publico/subcategorias").with(anonymous())).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Los avisos del broker de logística no piden token")
        void avisosDeLogistica() throws Exception {
            mockMvc.perform(post("/logistica/eventos/inicio-ruta").with(anonymous())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "idRuta", UUID.randomUUID(),
                                    "idsDonaciones", List.of(idDonacion),
                                    "urlMapaInteractivo", "https://donatrack.org/mapa/ruta-1"))))
                    .andExpect(status().isOk());

            verify(inicioDeRuta).procesar(any());
        }
    }

    // ── Rol por ruta ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Rol por ruta")
    class RolPorRuta {

        @Test
        @DisplayName("Un chofer no opera sobre Donaciones: 403 con el ErrorResponseDTO")
        void chofer() throws Exception {
            mockMvc.perform(get("/donaciones").with(TokensDePrueba.chofer()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.message").value(containsString("permiso")));
        }

        @Test
        @DisplayName("El listado de donantes es solo de la administración")
        void listadoDeDonantes() throws Exception {
            when(donantes.obtenerDonantes(any(), any(), any()))
                    .thenReturn(PaginaResponseDTO.desde(new Pagina<>(List.of(), 0, 20, 0)));

            mockMvc.perform(get("/donantes").with(TokensDePrueba.donante(idDonante, EMAIL_DONANTE)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/donantes").param("email", EMAIL_DONANTE)
                            .with(TokensDePrueba.entidad(idEntidad, EMAIL_ENTIDAD)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/donantes").with(TokensDePrueba.administrador()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Asignar una donación es solo de la administración (requisito 17)")
        void asignar() throws Exception {
            String cuerpo = objectMapper.writeValueAsString(Map.of("idEntidadBeneficiaria", idEntidad));

            mockMvc.perform(patch("/donaciones/{id}/asignar", idDonacion)
                            .with(TokensDePrueba.entidad(idEntidad, EMAIL_ENTIDAD))
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isForbidden());

            verify(comandos, never()).asignar(any(), any());
        }

        @Test
        @DisplayName("Cambiar estados, ver la bandeja, dar de baja e importar el CSV es de la administración")
        void operacionesDeAdministracion() throws Exception {
            var donante = TokensDePrueba.donante(idDonante, EMAIL_DONANTE);

            mockMvc.perform(get("/donaciones/candidatos").with(TokensDePrueba.entidad(idEntidad, EMAIL_ENTIDAD)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(patch("/donaciones/{id}/estado", idDonacion).with(donante)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"VENCIDA\"}"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(delete("/donantes/{id}", idDonante).with(donante))
                    .andExpect(status().isForbidden());
            mockMvc.perform(multipart("/donantes/import")
                            .file(new MockMultipartFile("file", "d.csv", "text/csv", "x".getBytes()))
                            .with(donante))
                    .andExpect(status().isForbidden());

            verify(donantes, never()).eliminar(any());
            verify(importacion, never()).importarYResumir(any());
        }
    }

    // ── Persona donante: solo lo suyo ───────────────────────────────────────

    @Nested
    @DisplayName("Persona donante: solo lo suyo")
    class Donante {

        @Test
        @DisplayName("Ve su perfil y no el de otra persona")
        void perfil() throws Exception {
            when(donantes.obtenerDonante(idDonante)).thenReturn(donanteCreado());
            var token = TokensDePrueba.donante(idDonante, EMAIL_DONANTE);

            mockMvc.perform(get("/donantes/{id}", idDonante).with(token)).andExpect(status().isOk());
            mockMvc.perform(get("/donantes/{id}", otroDonante).with(token))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(containsString("tu propio perfil")));
            mockMvc.perform(put("/donantes/{id}", otroDonante).with(token)
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());

            verify(donantes, never()).obtenerDonante(otroDonante);
            verify(donantes, never()).actualizar(eq(otroDonante), any());
        }

        @Test
        @DisplayName("El listado de donaciones queda limitado a las suyas aunque no lo pida")
        void listadoLimitado() throws Exception {
            when(consultas.obtenerDonaciones(any(), any())).thenReturn(paginaVacia());

            mockMvc.perform(get("/donaciones").param("estado", "EN_DEPOSITO")
                            .with(TokensDePrueba.donante(idDonante, EMAIL_DONANTE)))
                    .andExpect(status().isOk());

            verify(consultas).obtenerDonaciones(
                    new FiltroDonaciones("EN_DEPOSITO", idDonante, null, null, false), new SolicitudPagina(0, 20));
        }

        @Test
        @DisplayName("Pedir las donaciones de otra persona responde 403")
        void listadoDeOtro() throws Exception {
            mockMvc.perform(get("/donaciones").param("idDonante", otroDonante.toString())
                            .with(TokensDePrueba.donante(idDonante, EMAIL_DONANTE)))
                    .andExpect(status().isForbidden());

            verify(consultas, never()).obtenerDonaciones(any(), any());
        }

        @Test
        @DisplayName("Ve el detalle de su donación y no el de una ajena")
        void detalle() throws Exception {
            var token = TokensDePrueba.donante(idDonante, EMAIL_DONANTE);

            when(consultas.obtenerPorId(idDonacion)).thenReturn(donacion(idDonante, null));
            mockMvc.perform(get("/donaciones/{id}", idDonacion).with(token)).andExpect(status().isOk());

            when(consultas.obtenerPorId(idDonacion)).thenReturn(donacion(otroDonante, null));
            mockMvc.perform(get("/donaciones/{id}", idDonacion).with(token)).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Registra donaciones a su nombre y no a nombre de otra persona")
        void registrarDonacion() throws Exception {
            when(segmentador.registrar(any())).thenReturn(List.of(donacion(idDonante, null)));
            var token = TokensDePrueba.donante(idDonante, EMAIL_DONANTE);

            mockMvc.perform(post("/donaciones").with(token)
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpoDonacion(otroDonante)))
                    .andExpect(status().isForbidden());
            verify(segmentador, never()).registrar(any());

            mockMvc.perform(post("/donaciones").with(token)
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpoDonacion(idDonante)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("La administración registra donaciones a nombre de cualquier donante (requisito 15)")
        void administracionRegistraDonaciones() throws Exception {
            when(segmentador.registrar(any())).thenReturn(List.of(donacion(otroDonante, null)));

            mockMvc.perform(post("/donaciones").with(TokensDePrueba.administrador())
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpoDonacion(otroDonante)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Sin perfil registrado todavía, no puede ver donaciones: tiene que completar el registro")
        void sinPerfil() throws Exception {
            mockMvc.perform(get("/donaciones").with(TokensDePrueba.donanteSinPerfil(EMAIL_DONANTE, "HUMANA")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(containsString("completá el registro")));
        }
    }

    // ── Entidad beneficiaria: solo lo suyo ──────────────────────────────────

    @Nested
    @DisplayName("Entidad beneficiaria: solo lo suyo")
    class Entidad {

        @Test
        @DisplayName("El listado de donaciones queda limitado a las asignadas a ella")
        void listadoLimitado() throws Exception {
            when(consultas.obtenerDonaciones(any(), any())).thenReturn(paginaVacia());

            mockMvc.perform(get("/donaciones").with(TokensDePrueba.entidad(idEntidad, EMAIL_ENTIDAD)))
                    .andExpect(status().isOk());

            verify(consultas).obtenerDonaciones(
                    new FiltroDonaciones(null, null, null, idEntidad, false), new SolicitudPagina(0, 20));
        }

        @Test
        @DisplayName("Ve el detalle de una donación asignada a ella y no de una asignada a otra")
        void detalle() throws Exception {
            var token = TokensDePrueba.entidad(idEntidad, EMAIL_ENTIDAD);

            when(consultas.obtenerPorId(idDonacion)).thenReturn(donacion(idDonante, idEntidad));
            mockMvc.perform(get("/donaciones/{id}", idDonacion).with(token)).andExpect(status().isOk());

            when(consultas.obtenerPorId(idDonacion)).thenReturn(donacion(idDonante, otraEntidad));
            mockMvc.perform(get("/donaciones/{id}", idDonacion).with(token)).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Gestiona su entidad y sus campañas, no las de otra")
        void suEntidad() throws Exception {
            when(entidades.obtenerPorId(idEntidad)).thenReturn(entidadCreada());
            var token = TokensDePrueba.entidad(idEntidad, EMAIL_ENTIDAD);

            mockMvc.perform(get("/entidades/{id}", idEntidad).with(token)).andExpect(status().isOk());
            mockMvc.perform(get("/entidades/{id}", otraEntidad).with(token)).andExpect(status().isForbidden());
            mockMvc.perform(post("/entidades/{id}/campanias", otraEntidad).with(token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "descripcionGeneral", "Colecta",
                                    "fechaInicio", LocalDate.now().toString(),
                                    "fechaFin", LocalDate.now().plusMonths(1).toString()))))
                    .andExpect(status().isForbidden());

            verify(entidades, never()).agregarCampaniaAEntidad(eq(otraEntidad), any());
        }

        @Test
        @DisplayName("Una persona donante no ve entidades por id (para eso está el listado público)")
        void donanteNoVeEntidades() throws Exception {
            mockMvc.perform(get("/entidades/{id}", idEntidad).with(TokensDePrueba.donante(idDonante, EMAIL_DONANTE)))
                    .andExpect(status().isForbidden());
        }
    }

    // ── Alta de perfiles ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Alta de perfiles: cada cuenta registra el suyo, una vez")
    class AltaDePerfiles {

        @Test
        @DisplayName("Una cuenta de donante sin perfil registra el suyo con el email de la cuenta")
        void donanteRegistraElSuyo() throws Exception {
            when(donantes.registrar(any())).thenReturn(donanteCreado());

            mockMvc.perform(post("/donantes").with(TokensDePrueba.donanteSinPerfil(EMAIL_DONANTE, "HUMANA"))
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpoAltaHumana(EMAIL_DONANTE)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("El email se compara normalizado: mayúsculas y espacios no importan")
        void emailNormalizado() throws Exception {
            when(donantes.registrar(any())).thenReturn(donanteCreado());

            mockMvc.perform(post("/donantes").with(TokensDePrueba.donanteSinPerfil(" Juan@Example.com ", "HUMANA"))
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpoAltaHumana(EMAIL_DONANTE)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("No puede registrar un perfil con otro email")
        void otroEmail() throws Exception {
            mockMvc.perform(post("/donantes").with(TokensDePrueba.donanteSinPerfil(EMAIL_DONANTE, "HUMANA"))
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpoAltaHumana("otra@example.com")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(containsString("email")));

            verify(donantes, never()).registrar(any());
        }

        @Test
        @DisplayName("Una cuenta de persona jurídica no registra un perfil de persona humana")
        void otroTipo() throws Exception {
            mockMvc.perform(post("/donantes").with(TokensDePrueba.donanteSinPerfil(EMAIL_DONANTE, "JURIDICA"))
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpoAltaHumana(EMAIL_DONANTE)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(containsString("jurídica")));
        }

        @Test
        @DisplayName("Una cuenta que ya tiene perfil no registra otro: 409")
        void yaTienePerfil() throws Exception {
            mockMvc.perform(post("/donantes").with(TokensDePrueba.donante(idDonante, EMAIL_DONANTE))
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpoAltaHumana(EMAIL_DONANTE)))
                    .andExpect(status().isConflict());

            verify(donantes, never()).registrar(any());
        }

        @Test
        @DisplayName("La administración da de alta a cualquier persona donante (requisito 15)")
        void administracionDaDeAlta() throws Exception {
            when(donantes.registrarPorAdministracion(any())).thenReturn(donanteCreado());

            mockMvc.perform(post("/donantes").with(TokensDePrueba.administrador())
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpoAltaHumana("otra@example.com")))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Una cuenta de entidad registra la suya si tiene su email como representante")
        void entidadRegistraLaSuya() throws Exception {
            when(entidades.guardar(any())).thenReturn(entidadCreada());
            var token = TokensDePrueba.entidadSinPerfil(EMAIL_ENTIDAD);

            mockMvc.perform(post("/entidades").with(token)
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpoAltaEntidad("otra@example.org")))
                    .andExpect(status().isForbidden());
            verify(entidades, never()).guardar(any());

            mockMvc.perform(post("/entidades").with(token)
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpoAltaEntidad(EMAIL_ENTIDAD)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Una persona donante no registra entidades")
        void donanteNoRegistraEntidades() throws Exception {
            mockMvc.perform(post("/entidades").with(TokensDePrueba.donanteSinPerfil(EMAIL_DONANTE, "HUMANA"))
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpoAltaEntidad(EMAIL_DONANTE)))
                    .andExpect(status().isForbidden());
        }
    }
}
