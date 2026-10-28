package ar.utn.donatrack.donaciones.integracion;

import ar.utn.donatrack.donaciones.clientes.BrokerLogisticaClient;
import ar.utn.donatrack.donaciones.clientes.IncentivosClient;
import ar.utn.donatrack.donaciones.clientes.NotificacionClient;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioDonaciones;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioRecepcionesEntidad;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioCandidatosAsignacion;
import ar.utn.donatrack.donaciones.models.asignacion.RankingPrecalculado;
import ar.utn.donatrack.donaciones.services.AsignacionBatchService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Recorre el flujo principal por la API y contra la base (H2): alta de persona
 * donante y de entidad, registro de una donación y asignación.
 *
 * Lo que se verifica no se ve en los tests unitarios con repositorios simulados:
 * que con la transacción en el service los cambios de la entidad gestionada
 * llegan a la base, que la versión avanza y que el contador desnormalizado de
 * recepciones se mueve en la misma transacción que la asignación.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:flujo_donacion;DB_CLOSE_DELAY=-1;MODE=PostgreSQL")
@AutoConfigureMockMvc
@DisplayName("Integración: flujo de una donación por la API y contra la base")
class FlujoDeDonacionIntegracionTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private RepositorioDonaciones donaciones;
    @Autowired private RepositorioRecepcionesEntidad recepciones;
    @Autowired private RepositorioCandidatosAsignacion candidatos;
    @Autowired private AsignacionBatchService matchmaking;

    @MockitoBean private NotificacionClient notificacionClient;
    @MockitoBean private IncentivosClient incentivosClient;
    @MockitoBean private BrokerLogisticaClient brokerLogisticaClient;

    private JsonNode enviar(org.springframework.test.web.servlet.RequestBuilder pedido) throws Exception {
        String cuerpo = mvc.perform(pedido).andReturn().getResponse().getContentAsString();
        return cuerpo.isBlank() ? null : json.readTree(cuerpo);
    }

    private UUID crearDonante() throws Exception {
        String cuerpo = """
                {"tipo":"HUMANA","tipoDocumento":"DNI","numeroDocumento":"30111222","email":"%s",
                 "nombre":"Ana","apellido":"Pérez","fechaNacimiento":"1990-05-10","genero":"FEMENINO",
                 "direccion":{"calle":"Medrano","numero":951,"codigoPostal":"1179",
                              "localidad":{"nombre":"CABA","provincia":{"nombre":"Buenos Aires"}}},
                 "medioContactoPredeterminado":{"tipo":"EMAIL","valor":"%s"}}
                """.formatted("ana" + UUID.randomUUID() + "@mail.com", "ana@mail.com");
        return UUID.fromString(enviar(post("/donantes").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .get("id").asText());
    }

    private UUID crearEntidad() throws Exception {
        String cuerpo = """
                {"razonSocial":"Comedor Los Pibes",
                 "direccion":{"calle":"Medrano","numero":951,"codigoPostal":"1179",
                              "localidad":{"nombre":"CABA","provincia":{"nombre":"Buenos Aires"}}},
                 "contactos":[{"tipo":"EMAIL","valor":"comedor@mail.com"}]}
                """;
        return UUID.fromString(enviar(post("/entidades").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .get("id").asText());
    }

    private UUID registrarDonacion(UUID idDonante) throws Exception {
        String cuerpo = """
                {"idDonante":"%s","descripcion":"Colecta",
                 "bienes":[{"subcategoria":"Arroz","cantidad":5,"unidad":"kg"}]}
                """.formatted(idDonante);
        return UUID.fromString(enviar(post("/donaciones").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .get(0).get("id").asText());
    }

    @Test
    @DisplayName("Asignar persiste la asignación, avanza la versión y suma una recepción a la entidad")
    void asignarPersisteYCuenta() throws Exception {
        UUID idDonacion = registrarDonacion(crearDonante());
        UUID idEntidad = crearEntidad();
        long versionInicial = donaciones.buscarPorId(idDonacion).orElseThrow().getVersion();

        mvc.perform(patch("/donaciones/{id}/asignar", idDonacion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idEntidadBeneficiaria\":\"" + idEntidad + "\"}"))
                .andExpect(status().isNoContent());

        Donacion asignada = donaciones.buscarPorId(idDonacion).orElseThrow();
        assertThat(asignada.estaEnEstado("ASIGNACION_REALIZADA")).isTrue();
        assertThat(asignada.getIdEntidadBeneficiaria()).isEqualTo(idEntidad);
        assertThat(asignada.getHistorialEstados()).hasSize(1);
        assertThat(asignada.getVersion()).isGreaterThan(versionInicial);
        assertThat(recepciones.contarRecepcionesDesde(idEntidad, FechaHoraArgentina.hoy().minusDays(1))).isEqualTo(1);
    }

    @Test
    @DisplayName("Los eventos de la donación llegan a sus oyentes: bandeja, avisos e incentivos")
    void eventosDeDominio() throws Exception {
        // Spring Data publica los eventos al guardar la donación; los oyentes en
        // la transacción limpian la bandeja y los de después del commit avisan.
        UUID idDonacion = registrarDonacion(crearDonante());
        verify(incentivosClient).notificarDonacionRegistrada(any(), eq("ana@mail.com"), eq("EMAIL"), eq(1), any(), any());
        UUID idEntidad = crearEntidad();
        matchmaking.ejecutarMatchmakingNocturno();
        assertThat(candidatos.obtenerTodos()).extracting(RankingPrecalculado::getIdDonacion).contains(idDonacion);

        mvc.perform(patch("/donaciones/{id}/asignar", idDonacion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idEntidadBeneficiaria\":\"" + idEntidad + "\"}"))
                .andExpect(status().isNoContent());

        assertThat(candidatos.obtenerTodos()).extracting(RankingPrecalculado::getIdDonacion).doesNotContain(idDonacion);
        verify(notificacionClient).enviarNotificacion(eq("ana@mail.com"), anyString(), eq("EMAIL"));
        verify(notificacionClient).enviarNotificacion(eq("comedor@mail.com"), anyString(), eq("EMAIL"));

        for (String estado : new String[] {"LISTA_PARA_ENTREGAR", "EN_TRASLADO", "ENTREGADA"}) {
            mvc.perform(patch("/donaciones/{id}/estado", idDonacion)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"estado\":\"" + estado + "\"}"))
                    .andExpect(status().isNoContent());
        }
        verify(incentivosClient).notificarDonacionExitosa(any(), eq("ana@mail.com"), eq("EMAIL"), any());
    }

    @Test
    @DisplayName("Un cambio de estado inválido no deja nada a medias")
    void cambioInvalidoNoPersiste() throws Exception {
        UUID idDonacion = registrarDonacion(crearDonante());

        mvc.perform(patch("/donaciones/{id}/estado", idDonacion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"ENTREGADA\"}"))
                .andExpect(status().isUnprocessableEntity());

        Donacion igual = donaciones.buscarPorId(idDonacion).orElseThrow();
        assertThat(igual.estaEnEstado("EN_DEPOSITO")).isTrue();
        assertThat(igual.getHistorialEstados()).isEmpty();
    }
}
