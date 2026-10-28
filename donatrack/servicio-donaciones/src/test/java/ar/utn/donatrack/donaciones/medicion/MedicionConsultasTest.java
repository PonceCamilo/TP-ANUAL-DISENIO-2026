package ar.utn.donatrack.donaciones.medicion;

import ar.utn.donatrack.donaciones.models.donante.Documento;
import ar.utn.donatrack.donaciones.clientes.BrokerLogisticaClient;
import ar.utn.donatrack.donaciones.clientes.IncentivosClient;
import ar.utn.donatrack.donaciones.clientes.NotificacionClient;
import ar.utn.donatrack.donaciones.dtos.request.AsignacionRequestDTO;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioDonaciones;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioEntidadesBeneficiarias;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioPersonasDonantes;
import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.contacto.Email;
import ar.utn.donatrack.donaciones.models.contacto.MedioDeContacto;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienGenerico;
import ar.utn.donatrack.donaciones.models.donante.PersonaHumana;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Campania;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.NecesidadExtraordinaria;
import ar.utn.donatrack.donaciones.services.AsignacionBatchService;
import ar.utn.donatrack.donaciones.services.DonacionComandosService;
import ar.utn.donatrack.donaciones.services.InactividadDonantesService;
import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Mide cuántas sentencias SQL ejecuta cada operación de lectura y escritura del
 * servicio, contra H2 en modo PostgreSQL y con un volumen fijo de datos.
 *
 * POR QUÉ EXISTE (Entrega 5): las desnormalizaciones y los índices se justifican
 * con números, no con intuición. Se corrió primero sobre el código de la
 * Entrega 4 para tener la línea base, y ahora fija con topes que la cantidad de
 * sentencias no vuelva a crecer con el volumen de datos.
 *
 * Cuenta sentencias preparadas (Hibernate Statistics): con escrituras por lotes,
 * un lote de INSERT del mismo tipo cuenta como una.
 *
 * Los resultados se imprimen y se guardan en target/medicion-consultas-{etiqueta}.txt
 * (la etiqueta se pasa con -Ddonatrack.medicion.etiqueta=..., por defecto "actual").
 *
 * Usa su propia base en memoria para no compartir datos con otros tests que
 * levantan el contexto, y mockea los clientes HTTP salientes: lo que se mide es
 * el acceso a datos de Donaciones, no la red.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:medicion;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=WARN"
})
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Medición - sentencias SQL por operación")
class MedicionConsultasTest {

    private static final int ENTIDADES = 20;
    private static final int DONACIONES_EN_DEPOSITO = 30;
    private static final int DONACIONES_ASIGNADAS = 30;
    private static final int DONANTES = 200;
    private static final int FILAS_CSV = 300;

    // ── Topes de sentencias (regresión) ───────────────────────────────────────
    // Línea base medida sobre el código de la Entrega 4 y valor medido después de
    // las optimizaciones de la Entrega 5. El tope deja margen sobre lo medido;
    // si una consulta vuelve a crecer con el volumen de datos, el test falla.
    /** Línea base 2.603, medido 13. */
    private static final int TOPE_RANKING_A_DEMANDA = 30;
    /** Línea base 75.091, medido 192 (guarda 30 rankings). */
    private static final int TOPE_MATCHMAKING_NOCTURNO = 400;
    /** Línea base 3.000, medido 9. */
    private static final int TOPE_BANDEJA = 20;
    /** Línea base 121 (donaciones) y 401 (donantes), medido 3-4. */
    private static final int TOPE_LISTADO = 10;
    /** Línea base 401, medido 3. */
    private static final int TOPE_INACTIVIDAD = 10;
    /** Línea base 20, medido 7. */
    private static final int TOPE_REGISTRO = 15;
    /** Línea base 13, medido 23: subió por mantener el contador de recepciones. */
    private static final int TOPE_ASIGNACION = 35;
    /** Línea base 1.200 (altas) y 900 (actualizaciones), medido 6-7. */
    private static final int TOPE_IMPORTACION = 30;

    @Autowired private MockMvc mvc;
    @Autowired private EntityManagerFactory emf;
    @Autowired private RepositorioPersonasDonantes donantes;
    @Autowired private RepositorioEntidadesBeneficiarias entidades;
    @Autowired private RepositorioDonaciones donaciones;
    @Autowired private DonacionComandosService donacionComandos;
    @Autowired private AsignacionBatchService asignacionBatchService;
    @Autowired private InactividadDonantesService inactividadDonantesService;

    @MockitoBean private NotificacionClient notificacionClient;
    @MockitoBean private IncentivosClient incentivosClient;
    @MockitoBean private BrokerLogisticaClient brokerLogisticaClient;

    private Statistics estadisticas;
    private final Map<String, long[]> resultados = new LinkedHashMap<>();
    private final List<UUID> idsDonantes = new ArrayList<>();
    private final List<UUID> idsEntidades = new ArrayList<>();
    private final List<UUID> idsEnDeposito = new ArrayList<>();
    private byte[] csv;

    @FunctionalInterface
    private interface Accion {
        void ejecutar() throws Exception;
    }

    // ── Datos ──────────────────────────────────────────────────────────────────

    @BeforeAll
    void sembrarDatos() throws Exception {
        estadisticas = emf.unwrap(SessionFactory.class).getStatistics();

        for (int i = 0; i < DONANTES; i++) {
            PersonaHumana persona = PersonaHumana.builder()
                    .id(UUID.randomUUID())
                    .nombre("Donante")
                    .apellido("N" + i)
                    .documento(Documento.de("DNI", String.valueOf(30_000_000 + i)))
                    .email("donante" + i + "@mail.com")
                    // La mitad interactuó hace 30 días: son las que detecta el proceso de inactividad.
                    .ultimaInteraccion(i % 2 == 0
                            ? FechaHoraArgentina.ahora()
                            : FechaHoraArgentina.ahora().minusDays(30))
                    .build();
            persona.definirContactoPredeterminado(Email.builder().valor("donante" + i + "@mail.com").build());
            donantes.guardar(persona);
            idsDonantes.add(persona.getId());
        }

        for (int i = 0; i < ENTIDADES; i++) {
            NecesidadExtraordinaria necesidad = new NecesidadExtraordinaria();
            // La mitad de las entidades necesita arroz: son las compatibles con las donaciones.
            necesidad.setNombre(i % 2 == 0 ? "Arroz para el comedor" : "Fideos secos");
            necesidad.setCantidadObjetivo(50);

            UUID idEntidad = UUID.randomUUID();
            Campania campania = new Campania();
            campania.setIdCampania(UUID.randomUUID());
            campania.setIdEntidad(idEntidad);
            campania.setFechaInicio(LocalDate.now());
            campania.setFechaFin(LocalDate.now().plusMonths(3));
            campania.agregarNecesidad(necesidad);

            List<MedioDeContacto> contactos = new ArrayList<>();
            contactos.add(Email.builder().valor("entidad" + i + "@mail.com").build());
            EntidadBeneficiaria entidad = EntidadBeneficiaria.builder()
                    .id(idEntidad)
                    .razonSocial("Entidad " + i)
                    .contactos(contactos)
                    .representantes(new ArrayList<>())
                    .campanias(new ArrayList<>(List.of(campania)))
                    .build();
            entidades.guardar(entidad);
            idsEntidades.add(idEntidad);
        }

        List<Donacion> nuevas = new ArrayList<>();
        for (int i = 0; i < DONACIONES_EN_DEPOSITO + DONACIONES_ASIGNADAS; i++) {
            nuevas.add(donacionDeArroz(idsDonantes.get(i % 10)));
        }
        donaciones.cargarDonaciones(nuevas);
        for (int i = 0; i < nuevas.size(); i++) {
            if (i < DONACIONES_ASIGNADAS) {
                AsignacionRequestDTO asignacion = new AsignacionRequestDTO();
                asignacion.setIdEntidadBeneficiaria(idsEntidades.get(i % ENTIDADES));
                donacionComandos.asignar(nuevas.get(i).getId(), asignacion);
            } else {
                idsEnDeposito.add(nuevas.get(i).getId());
            }
        }

        StringBuilder filas = new StringBuilder("TipoPersona,TipoDoc,Documento,Nombre/Razón Social,Email,Teléfono\n");
        for (int i = 0; i < FILAS_CSV; i++) {
            filas.append("HUMANA,DNI,").append(40_000_000 + i).append(",Persona CSV").append(i)
                    .append(",csv").append(i).append("@mail.com,+54 11 5555-").append(1000 + i).append('\n');
        }
        csv = filas.toString().getBytes(StandardCharsets.UTF_8);
    }

    private Donacion donacionDeArroz(UUID idDonante) {
        Donacion donacion = new Donacion();
        donacion.setIdDonante(idDonante);
        donacion.setSubcategoria(new Subcategoria("Arroz"));
        donacion.setDescripcion("Arroz");
        donacion.agregarBien(BienGenerico.builder()
                .subcategoria(new Subcategoria("Arroz"))
                .descripcion("Paquete de arroz")
                .cantidad(5)
                .unidad("kg")
                .build());
        return donacion;
    }

    private long medir(String operacion, Accion accion) throws Exception {
        estadisticas.clear();
        long inicio = System.nanoTime();
        accion.ejecutar();
        long milisegundos = (System.nanoTime() - inicio) / 1_000_000;
        long sentencias = estadisticas.getPrepareStatementCount();
        resultados.put(operacion, new long[]{sentencias, milisegundos});
        return sentencias;
    }

    // ── Lecturas ───────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Ranking a demanda de una donación")
    void rankingADemanda() throws Exception {
        long sentencias = medir("GET /donaciones/{id}/candidatos", () ->
                mvc.perform(get("/donaciones/{id}/candidatos", idsEnDeposito.getFirst()))
                        .andExpect(status().isOk()));
        assertThat(sentencias).isLessThanOrEqualTo(TOPE_RANKING_A_DEMANDA);
    }

    @Test
    @Order(2)
    @DisplayName("Matchmaking nocturno de todas las donaciones en depósito")
    void matchmakingNocturno() throws Exception {
        long sentencias = medir("Matchmaking nocturno (batch)", () ->
                asignacionBatchService.ejecutarMatchmakingNocturno());
        assertThat(sentencias).isLessThanOrEqualTo(TOPE_MATCHMAKING_NOCTURNO);
    }

    @Test
    @Order(3)
    @DisplayName("Bandeja de candidatos precalculados")
    void bandejaDeCandidatos() throws Exception {
        long sentencias = medir("GET /donaciones/candidatos", () ->
                mvc.perform(get("/donaciones/candidatos")).andExpect(status().isOk()));
        assertThat(sentencias).isLessThanOrEqualTo(TOPE_BANDEJA);
    }

    @Test
    @Order(4)
    @DisplayName("Listado de donaciones de un donante")
    void donacionesDeUnDonante() throws Exception {
        long sentencias = medir("GET /donaciones?idDonante=", () ->
                mvc.perform(get("/donaciones").param("idDonante", idsDonantes.getFirst().toString()))
                        .andExpect(status().isOk()));
        assertThat(sentencias).isLessThanOrEqualTo(TOPE_LISTADO);
    }

    @Test
    @Order(5)
    @DisplayName("Listado de donaciones por estado")
    void donacionesPorEstado() throws Exception {
        long sentencias = medir("GET /donaciones?estado=EN_DEPOSITO", () ->
                mvc.perform(get("/donaciones").param("estado", "EN_DEPOSITO"))
                        .andExpect(status().isOk()));
        assertThat(sentencias).isLessThanOrEqualTo(TOPE_LISTADO);
    }

    @Test
    @Order(6)
    @DisplayName("Listado de todas las donaciones")
    void todasLasDonaciones() throws Exception {
        long sentencias = medir("GET /donaciones", () ->
                mvc.perform(get("/donaciones")).andExpect(status().isOk()));
        assertThat(sentencias).isLessThanOrEqualTo(TOPE_LISTADO);
    }

    @Test
    @Order(7)
    @DisplayName("Listado de donantes")
    void listadoDeDonantes() throws Exception {
        long sentencias = medir("GET /donantes", () ->
                mvc.perform(get("/donantes")).andExpect(status().isOk()));
        assertThat(sentencias).isLessThanOrEqualTo(TOPE_LISTADO);
    }

    @Test
    @Order(8)
    @DisplayName("Proceso nocturno de inactividad")
    void procesoDeInactividad() throws Exception {
        long sentencias = medir("Proceso de inactividad (batch)", () ->
                inactividadDonantesService.notificarDonantesInactivos());
        assertThat(sentencias).isLessThanOrEqualTo(TOPE_INACTIVIDAD);
    }

    // ── Escrituras ─────────────────────────────────────────────────────────────

    @Test
    @Order(9)
    @DisplayName("Registro de una carga con 4 bienes")
    void registroDeDonacion() throws Exception {
        String cuerpo = """
                {"idDonante": "%s", "descripcion": "Colecta", "bienes": [
                  {"subcategoria": "Arroz", "descripcion": "Arroz", "cantidad": 2, "unidad": "kg", "fechaVencimiento": "2030-01-01"},
                  {"subcategoria": "Arroz", "descripcion": "Arroz", "cantidad": 3, "unidad": "kg", "fechaVencimiento": "2030-01-01"},
                  {"subcategoria": "Sillas", "descripcion": "Silla", "cantidad": 4, "unidad": "unidades", "esNuevo": false},
                  {"subcategoria": "Frazadas", "descripcion": "Frazada", "cantidad": 6, "unidad": "unidades"}
                ]}""".formatted(idsDonantes.getFirst());
        long sentencias = medir("POST /donaciones (4 bienes)", () ->
                mvc.perform(post("/donaciones").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                        .andExpect(status().isCreated()));
        assertThat(sentencias).isLessThanOrEqualTo(TOPE_REGISTRO);
    }

    @Test
    @Order(10)
    @DisplayName("Asignación de una donación")
    void asignacion() throws Exception {
        String cuerpo = "{\"idEntidadBeneficiaria\": \"%s\"}".formatted(idsEntidades.getFirst());
        long sentencias = medir("PATCH /donaciones/{id}/asignar", () ->
                mvc.perform(patch("/donaciones/{id}/asignar", idsEnDeposito.getLast())
                                .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                        .andExpect(status().isNoContent()));
        assertThat(sentencias).isLessThanOrEqualTo(TOPE_ASIGNACION);
    }

    @Test
    @Order(11)
    @DisplayName("Importación CSV de filas nuevas")
    void importacionCsvNuevas() throws Exception {
        long sentencias = medir("POST /donantes/import (" + FILAS_CSV + " altas)", () ->
                mvc.perform(multipart("/donantes/import")
                                .file(new MockMultipartFile("file", "donantes.csv", "text/csv", csv)))
                        .andExpect(status().isOk()));
        assertThat(sentencias).isLessThanOrEqualTo(TOPE_IMPORTACION);
    }

    @Test
    @Order(12)
    @DisplayName("Reimportación del mismo CSV (todas actualizaciones)")
    void importacionCsvActualizaciones() throws Exception {
        long sentencias = medir("POST /donantes/import (" + FILAS_CSV + " actualizaciones)", () ->
                mvc.perform(multipart("/donantes/import")
                                .file(new MockMultipartFile("file", "donantes.csv", "text/csv", csv)))
                        .andExpect(status().isOk()));
        assertThat(sentencias).isLessThanOrEqualTo(TOPE_IMPORTACION);
    }

    // ── Informe ────────────────────────────────────────────────────────────────

    @AfterAll
    void informar() throws IOException {
        String etiqueta = System.getProperty("donatrack.medicion.etiqueta", "actual");
        StringBuilder informe = new StringBuilder()
                .append("Medición de sentencias SQL (").append(etiqueta).append(")\n")
                .append("Datos: ").append(DONANTES).append(" donantes, ").append(ENTIDADES).append(" entidades, ")
                .append(DONACIONES_EN_DEPOSITO + DONACIONES_ASIGNADAS).append(" donaciones (")
                .append(DONACIONES_ASIGNADAS).append(" asignadas)\n\n")
                .append(String.format("%-48s %10s %8s%n", "Operación", "Sentencias", "ms"));
        resultados.forEach((operacion, valores) ->
                informe.append(String.format("%-48s %10d %8d%n", operacion, valores[0], valores[1])));
        System.out.println(informe);
        Path destino = Path.of("target", "medicion-consultas-" + etiqueta + ".txt");
        Files.createDirectories(destino.getParent());
        Files.writeString(destino, informe.toString(), StandardCharsets.UTF_8);
    }
}
