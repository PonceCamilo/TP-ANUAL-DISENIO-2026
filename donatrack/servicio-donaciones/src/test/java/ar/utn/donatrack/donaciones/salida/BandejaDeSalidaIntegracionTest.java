package ar.utn.donatrack.donaciones.salida;

import ar.utn.donatrack.donaciones.interfaces.integracion.NotificacionTransport;
import ar.utn.donatrack.donaciones.integracion.incentivos.EnviadorIncentivos;
import ar.utn.donatrack.donaciones.integracion.notificaciones.SolicitudNotificacionMensaje;
import ar.utn.donatrack.donaciones.repositories.jpa.MensajeSalienteJpaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La bandeja de salida contra la base y con los clientes reales: los avisos se
 * guardan con el cambio que los origina y el relay los envía (los transportes
 * hacia los otros servicios se simulan).
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:bandeja_salida;DB_CLOSE_DELAY=-1;MODE=PostgreSQL")
@AutoConfigureMockMvc
@DisplayName("Integración: bandeja de salida (outbox)")
class BandejaDeSalidaIntegracionTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private MensajeSalienteJpaRepository mensajes;
    @Autowired private RelayDeMensajes relay;

    @MockitoBean private NotificacionTransport transporte;
    /** Espía del enviador real: conserva sus destinos y solo se simula el envío HTTP. */
    @MockitoSpyBean private EnviadorIncentivos enviadorIncentivos;

    @BeforeEach
    void vaciar() {
        mensajes.deleteAll();
        doNothing().when(enviadorIncentivos).enviar(anyString(), anyString());
    }

    private JsonNode enviar(RequestBuilder pedido) throws Exception {
        String cuerpo = mvc.perform(pedido).andReturn().getResponse().getContentAsString();
        return cuerpo.isBlank() ? null : json.readTree(cuerpo);
    }

    private UUID donacionAsignada() throws Exception {
        String donante = """
                {"tipo":"HUMANA","tipoDocumento":"DNI","numeroDocumento":"30111222","email":"%s",
                 "nombre":"Ana","apellido":"Pérez","fechaNacimiento":"1990-05-10","genero":"FEMENINO",
                 "direccion":{"calle":"Medrano","numero":951,"codigoPostal":"1179",
                              "localidad":{"nombre":"CABA","provincia":{"nombre":"Buenos Aires"}}},
                 "medioContactoPredeterminado":{"tipo":"EMAIL","valor":"ana@mail.com"}}
                """.formatted("ana" + UUID.randomUUID() + "@mail.com");
        UUID idDonante = UUID.fromString(enviar(post("/donantes").contentType(MediaType.APPLICATION_JSON).content(donante))
                .get("id").asText());
        String entidad = """
                {"razonSocial":"Comedor Los Pibes",
                 "direccion":{"calle":"Medrano","numero":951,"codigoPostal":"1179",
                              "localidad":{"nombre":"CABA","provincia":{"nombre":"Buenos Aires"}}},
                 "contactos":[{"tipo":"EMAIL","valor":"comedor@mail.com"}]}
                """;
        UUID idEntidad = UUID.fromString(enviar(post("/entidades").contentType(MediaType.APPLICATION_JSON).content(entidad))
                .get("id").asText());
        String donacion = """
                {"idDonante":"%s","descripcion":"Colecta","bienes":[{"subcategoria":"Arroz","cantidad":5,"unidad":"kg"}]}
                """.formatted(idDonante);
        UUID idDonacion = UUID.fromString(enviar(post("/donaciones").contentType(MediaType.APPLICATION_JSON)
                .content(donacion)).get(0).get("id").asText());
        mvc.perform(patch("/donaciones/{id}/asignar", idDonacion).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idEntidadBeneficiaria\":\"" + idEntidad + "\"}"))
                .andExpect(status().isNoContent());
        return idDonacion;
    }

    @Test
    @DisplayName("Los avisos quedan guardados con el cambio y el relay los envía")
    void guardaYEnvia() throws Exception {
        donacionAsignada();

        // Registro (incentivos) + asignación (donante y entidad): todavía no salió nada.
        assertThat(mensajes.findByDestino(Destinos.NOTIFICACION)).hasSize(2);
        assertThat(mensajes.findByDestino(Destinos.INCENTIVOS_DONACION_REGISTRADA)).singleElement()
                .satisfies(m -> assertThat(m.getPayload()).contains("\"fecha\":\"20"));
        verify(transporte, never()).enviar(any());

        assertThat(relay.enviarPendientes()).isEqualTo(3);

        verify(transporte).enviar(argThat((SolicitudNotificacionMensaje s) ->
                s.destinatario().equals("comedor@mail.com") && s.medio().equals("EMAIL")));
        verify(transporte).enviar(argThat((SolicitudNotificacionMensaje s) ->
                s.destinatario().equals("ana@mail.com") && s.mensaje().contains("Comedor Los Pibes")));
        verify(enviadorIncentivos).enviar(eq(Destinos.INCENTIVOS_DONACION_REGISTRADA), anyString());
        assertThat(mensajes.countByEstado(EstadoMensaje.ENVIADO)).isEqualTo(3);
        assertThat(mensajes.countByEstado(EstadoMensaje.PENDIENTE)).isZero();
    }

    @Test
    @DisplayName("Si el servicio de notificaciones está caído, los avisos esperan su reintento")
    void reintentaDespues() throws Exception {
        doThrow(new IllegalStateException("notificaciones caído")).when(transporte).enviar(any());
        donacionAsignada();

        relay.enviarPendientes();

        assertThat(mensajes.findByDestino(Destinos.NOTIFICACION)).allSatisfy(m -> {
            assertThat(m.getEstado()).isEqualTo(EstadoMensaje.PENDIENTE);
            assertThat(m.getIntentos()).isEqualTo(1);
            assertThat(m.getUltimoError()).isEqualTo("notificaciones caído");
            assertThat(m.getProximoIntento()).isAfter(m.getCreado());
        });
        // Hasta su próximo intento, no se vuelven a tomar.
        assertThat(relay.enviarPendientes()).isZero();
    }

    @Test
    @DisplayName("Un cambio que falla no deja avisos")
    void cambioFallidoSinAvisos() throws Exception {
        UUID idDonacion = donacionAsignada();
        mensajes.deleteAll();

        mvc.perform(patch("/donaciones/{id}/estado", idDonacion).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"ENTREGADA\"}"))
                .andExpect(status().isUnprocessableEntity());

        assertThat(mensajes.count()).isZero();
    }
}
