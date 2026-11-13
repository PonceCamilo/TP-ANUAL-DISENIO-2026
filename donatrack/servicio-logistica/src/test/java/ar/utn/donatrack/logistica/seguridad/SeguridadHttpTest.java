package ar.utn.donatrack.logistica.seguridad;

import ar.utn.donatrack.logistica.config.SeguridadConfig;
import ar.utn.donatrack.logistica.controllers.CamionController;
import ar.utn.donatrack.logistica.controllers.EntregaController;
import ar.utn.donatrack.logistica.controllers.MockProveedorRuteoController;
import ar.utn.donatrack.logistica.controllers.PlanificacionController;
import ar.utn.donatrack.logistica.controllers.RutaController;
import ar.utn.donatrack.logistica.dtos.response.EntregaResponseDTO;
import ar.utn.donatrack.logistica.dtos.response.LoteResponseDTO;
import ar.utn.donatrack.logistica.dtos.response.ParadaResponseDTO;
import ar.utn.donatrack.logistica.dtos.response.RutaResponseDTO;
import ar.utn.donatrack.logistica.interfaces.services.CamionServiceInterface;
import ar.utn.donatrack.logistica.interfaces.services.EntregaServiceInterface;
import ar.utn.donatrack.logistica.interfaces.services.PlanificacionServiceInterface;
import ar.utn.donatrack.logistica.models.entrega.EstadoEntrega;
import ar.utn.donatrack.logistica.models.planificacion.EstadoLote;
import ar.utn.donatrack.logistica.models.planificacion.EstadoRuta;

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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Quién puede hacer qué en Logística (Entrega 6). Usa la configuración de
 * seguridad real, sin el token de administración por defecto de los demás tests.
 */
@WebMvcTest(controllers = {CamionController.class, EntregaController.class, RutaController.class,
        PlanificacionController.class, MockProveedorRuteoController.class})
@Import({SeguridadConfig.class, ControlDeAcceso.class})
@DisplayName("Seguridad de Logística: roles, dueño de la entrega y puertas sin JWT")
class SeguridadHttpTest {
    static final String CLAVE_SERVICIOS = "clave-de-servicios-solo-para-las-pruebas";

    @Autowired MockMvc http;
    @Autowired ObjectMapper json;
    @MockitoBean CamionServiceInterface camiones;
    @MockitoBean EntregaServiceInterface entregas;
    @MockitoBean PlanificacionServiceInterface planificacion;

    private final UUID entidad = UUID.randomUUID();
    private final UUID otraEntidad = UUID.randomUUID();
    private final UUID idEntrega = UUID.randomUUID();
    private final UUID idRuta = UUID.randomUUID();

    @Nested
    @DisplayName("Sin token")
    class SinToken {
        @Test
        @DisplayName("Responde 401 con el formato de error del servicio")
        void respondeNoAutorizado() throws Exception {
            http.perform(get("/api/logistica/camiones"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string("WWW-Authenticate", "Bearer"))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.message").value("Falta el token de acceso o no es válido. Iniciá sesión de nuevo."));
        }

        @Test
        @DisplayName("El callback del proveedor de ruteo no pide JWT: usa su token de correlación en Authorization")
        void callbackConTokenDeCorrelacion() throws Exception {
            http.perform(post("/api/logistica/planificaciones/callback")
                            .header("Authorization", "Bearer token-de-correlacion")
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpoCallback()))
                    .andExpect(status().isOk());
            verify(planificacion).registrarCallback(any(), eq("token-de-correlacion"));
        }

        @Test
        @DisplayName("El proveedor de ruteo simulado no pide token (Logística se lo pide a sí misma)")
        void proveedorSimulado() throws Exception {
            http.perform(post("/ruteo/planificar").contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().is(not(401)))
                    .andExpect(status().is(not(403)));
        }
    }

    @Nested
    @DisplayName("Flota y rutas")
    class Flota {
        @Test
        @DisplayName("La flota es de la administración")
        void soloAdministracion() throws Exception {
            when(camiones.obtenerTodos()).thenReturn(List.of());
            http.perform(get("/api/logistica/camiones").with(TokensDePrueba.administrador())).andExpect(status().isOk());
            http.perform(get("/api/logistica/camiones").with(TokensDePrueba.chofer())).andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("No tenés permiso para hacer esta operación."));
            http.perform(post("/api/logistica/camiones/{id}/mantenimiento", UUID.randomUUID()).with(TokensDePrueba.entidad(entidad)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("El chofer informa la ubicación del camión e inicia su ruta; una entidad no")
        void chofer() throws Exception {
            String ubicacion = "{\"latitud\":-34.6,\"longitud\":-58.4}";
            http.perform(post("/api/logistica/camiones/{id}/ubicacion", UUID.randomUUID()).with(TokensDePrueba.chofer())
                    .contentType(MediaType.APPLICATION_JSON).content(ubicacion)).andExpect(status().isOk());
            http.perform(post("/api/logistica/camiones/{id}/ubicacion", UUID.randomUUID()).with(TokensDePrueba.entidad(entidad))
                    .contentType(MediaType.APPLICATION_JSON).content(ubicacion)).andExpect(status().isForbidden());
            http.perform(post("/api/logistica/rutas/{id}/iniciar", idRuta).with(TokensDePrueba.chofer())).andExpect(status().isOk());
            http.perform(post("/api/logistica/rutas/{id}/iniciar", idRuta).with(TokensDePrueba.donante())).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Una entidad sigue en el mapa solo las rutas que pasan por ella; el donante, las que le muestra el front")
        void seguimiento() throws Exception {
            when(planificacion.obtenerRuta(idRuta)).thenReturn(rutaQuePasaPor(entidad));
            http.perform(get("/api/logistica/rutas/{id}", idRuta).with(TokensDePrueba.entidad(entidad))).andExpect(status().isOk());
            http.perform(get("/api/logistica/rutas/{id}", idRuta).with(TokensDePrueba.entidad(otraEntidad)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Esta ruta no pasa por tu entidad."));
            http.perform(get("/api/logistica/rutas/{id}", idRuta).with(TokensDePrueba.donante())).andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Entregas")
    class Entregas {
        @Test
        @DisplayName("La entidad confirma la recepción de su entrega con las URLs de las fotos")
        void confirmaLaPropia() throws Exception {
            when(entregas.obtenerPorId(idEntrega)).thenReturn(entregaPara(entidad));
            http.perform(post("/api/logistica/entregas/{id}/confirmar", idEntrega).with(TokensDePrueba.entidad(entidad))
                            .contentType(MediaType.APPLICATION_JSON).content(fotos("https://fotos.example/recibido.jpg")))
                    .andExpect(status().isOk());
            verify(entregas).confirmar(eq(idEntrega), any());
        }

        @Test
        @DisplayName("Otra entidad, la administración o un chofer no pueden confirmarla")
        void nadieMasConfirma() throws Exception {
            when(entregas.obtenerPorId(idEntrega)).thenReturn(entregaPara(entidad));
            String cuerpo = fotos("https://fotos.example/recibido.jpg");
            http.perform(post("/api/logistica/entregas/{id}/confirmar", idEntrega).with(TokensDePrueba.entidad(otraEntidad))
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Esta entrega no es para tu entidad."));
            http.perform(post("/api/logistica/entregas/{id}/confirmar", idEntrega).with(TokensDePrueba.administrador())
                    .contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andExpect(status().isForbidden());
            http.perform(post("/api/logistica/entregas/{id}/confirmar", idEntrega).with(TokensDePrueba.chofer())
                    .contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andExpect(status().isForbidden());
            verify(entregas, never()).confirmar(any(), any());
        }

        @Test
        @DisplayName("Una entidad sin perfil vinculado no ve ni confirma entregas")
        void entidadSinPerfil() throws Exception {
            when(entregas.obtenerPorId(idEntrega)).thenReturn(entregaPara(entidad));
            http.perform(get("/api/logistica/entregas/{id}", idEntrega).with(TokensDePrueba.entidad(null)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Las fotos tienen que ser URLs http o https, hasta 10")
        void validaLasFotos() throws Exception {
            when(entregas.obtenerPorId(idEntrega)).thenReturn(entregaPara(entidad));
            http.perform(post("/api/logistica/entregas/{id}/confirmar", idEntrega).with(TokensDePrueba.entidad(entidad))
                    .contentType(MediaType.APPLICATION_JSON).content(fotos("comprobante en texto"))).andExpect(status().isBadRequest());
            String[] once = new String[11];
            java.util.Arrays.fill(once, "https://fotos.example/una.jpg");
            http.perform(post("/api/logistica/entregas/{id}/confirmar", idEntrega).with(TokensDePrueba.entidad(entidad))
                    .contentType(MediaType.APPLICATION_JSON).content(fotos(once))).andExpect(status().isBadRequest());
            verify(entregas, never()).confirmar(any(), any());
        }

        @Test
        @DisplayName("La entidad consulta solo sus entregas; el listado por estado es de la administración")
        void consultas() throws Exception {
            UUID donacion = UUID.randomUUID();
            when(entregas.obtenerPorDonacion(donacion)).thenReturn(entregaPara(entidad));
            http.perform(get("/api/logistica/entregas/por-donacion/{id}", donacion).with(TokensDePrueba.entidad(entidad)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.idEntidadBeneficiaria").value(entidad.toString()));
            http.perform(get("/api/logistica/entregas/por-donacion/{id}", donacion).with(TokensDePrueba.entidad(otraEntidad)))
                    .andExpect(status().isForbidden());
            http.perform(get("/api/logistica/entregas").param("estado", "NO_RECIBIDA").with(TokensDePrueba.entidad(entidad)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("El chofer marca una entrega como no recibida; el regreso al depósito lo decide la administración")
        void incidentes() throws Exception {
            String motivo = "{\"motivo\":\"ENTIDAD_AUSENTE\"}";
            http.perform(post("/api/logistica/entregas/{id}/no-recibida", idEntrega).with(TokensDePrueba.entidad(entidad))
                    .contentType(MediaType.APPLICATION_JSON).content(motivo)).andExpect(status().isForbidden());
            http.perform(post("/api/logistica/entregas/{id}/regreso-deposito", idEntrega).with(TokensDePrueba.chofer()))
                    .andExpect(status().isForbidden());
            http.perform(post("/api/logistica/entregas/{id}/regreso-deposito", idEntrega).with(TokensDePrueba.administrador()))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Planificación")
    class Planificacion {
        @Test
        @DisplayName("El broker planifica con la clave entre servicios; con otra clave, no")
        void claveDeServicios() throws Exception {
            when(planificacion.planificar(any())).thenReturn(List.of(LoteResponseDTO.builder()
                    .id(UUID.randomUUID()).estado(EstadoLote.COMPLETADO).cantidadDonaciones(1).rutas(List.of()).build()));

            http.perform(post("/api/logistica/planificaciones").header("X-DonaTrack-Servicio", CLAVE_SERVICIOS)
                    .contentType(MediaType.APPLICATION_JSON).content(cuerpoPlanificar())).andExpect(status().isOk());
            http.perform(post("/api/logistica/planificaciones").header("X-DonaTrack-Servicio", "otra-clave-de-al-menos-32-caracteres!!")
                    .contentType(MediaType.APPLICATION_JSON).content(cuerpoPlanificar())).andExpect(status().isUnauthorized());
            http.perform(post("/api/logistica/planificaciones").with(TokensDePrueba.donante())
                    .contentType(MediaType.APPLICATION_JSON).content(cuerpoPlanificar())).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("La clave entre servicios solo sirve para planificar")
        void laClaveNoAbreTodo() throws Exception {
            http.perform(get("/api/logistica/camiones").header("X-DonaTrack-Servicio", CLAVE_SERVICIOS))
                    .andExpect(status().isForbidden());
            http.perform(post("/api/logistica/planificaciones/reintentos").header("X-DonaTrack-Servicio", CLAVE_SERVICIOS))
                    .andExpect(status().isForbidden());
        }
    }

    private EntregaResponseDTO entregaPara(UUID entidadDeLaParada) {
        return EntregaResponseDTO.builder().id(idEntrega).idDonacion(UUID.randomUUID())
                .idEntidadBeneficiaria(entidadDeLaParada).estado(EstadoEntrega.EN_TRASLADO)
                .fotosComprobante(List.of()).historial(List.of()).build();
    }

    private RutaResponseDTO rutaQuePasaPor(UUID entidadDeLaParada) {
        return RutaResponseDTO.builder().id(idRuta).estado(EstadoRuta.INICIADA)
                .paradas(List.of(ParadaResponseDTO.builder().id(UUID.randomUUID()).orden(1)
                        .idEntidadBeneficiaria(entidadDeLaParada).entregas(List.of()).build()))
                .build();
    }

    private String fotos(String... urls) throws Exception {
        return json.writeValueAsString(Map.of("fotosComprobante", List.of(urls)));
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
        return json.writeValueAsString(Map.of(
                "camionesIds", List.of(UUID.randomUUID()),
                "donaciones", List.of(Map.of("idDonacion", UUID.randomUUID(), "idEntidadBeneficiaria", entidad,
                        "direccionEntrega", direccion()))));
    }

    private String cuerpoCallback() throws Exception {
        return json.writeValueAsString(Map.of("loteId", UUID.randomUUID(), "rutas", List.of(Map.of(
                "camionId", UUID.randomUUID(),
                "paradas", List.of(Map.of("orden", 1, "idEntidadBeneficiaria", entidad, "direccion", direccion(),
                        "donacionesIds", List.of(UUID.randomUUID())))))));
    }
}
