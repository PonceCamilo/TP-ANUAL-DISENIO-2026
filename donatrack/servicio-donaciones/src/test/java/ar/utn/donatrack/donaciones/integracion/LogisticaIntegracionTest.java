package ar.utn.donatrack.donaciones.integracion;

import ar.utn.donatrack.donaciones.models.donacion.CambioEstado;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioDonaciones;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.repositories.jpa.MensajeSalienteJpaRepository;
import ar.utn.donatrack.donaciones.salida.Destinos;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.transaction.support.TransactionOperations;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Los callbacks de logística contra la base: una ruta se aplica entera o no se
 * aplica, y un aviso repetido responde bien sin cambiar nada.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:logistica_integracion;DB_CLOSE_DELAY=-1;MODE=PostgreSQL")
@AutoConfigureMockMvc
@DisplayName("Integración: callbacks de logística")
class LogisticaIntegracionTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private RepositorioDonaciones donaciones;
    @Autowired private MensajeSalienteJpaRepository mensajes;
    @Autowired private TransactionOperations transaccion;

    private UUID idDonante;
    private UUID idEntidad;

    private JsonNode enviar(RequestBuilder pedido) throws Exception {
        String cuerpo = mvc.perform(pedido).andReturn().getResponse().getContentAsString();
        return cuerpo.isBlank() ? null : json.readTree(cuerpo);
    }

    @BeforeEach
    void crearDonanteYEntidad() throws Exception {
        String donante = """
                {"tipo":"HUMANA","tipoDocumento":"DNI","numeroDocumento":"30111222","email":"%s",
                 "nombre":"Ana","apellido":"Pérez","fechaNacimiento":"1990-05-10","genero":"FEMENINO",
                 "direccion":{"calle":"Medrano","numero":951,"codigoPostal":"1179",
                              "localidad":{"nombre":"CABA","provincia":{"nombre":"Buenos Aires"}}},
                 "medioContactoPredeterminado":{"tipo":"EMAIL","valor":"ana@mail.com"}}
                """.formatted("ana" + UUID.randomUUID() + "@mail.com");
        idDonante = UUID.fromString(enviar(post("/donantes").contentType(MediaType.APPLICATION_JSON).content(donante))
                .get("id").asText());
        String entidad = """
                {"razonSocial":"Comedor Los Pibes",
                 "direccion":{"calle":"Medrano","numero":951,"codigoPostal":"1179",
                              "localidad":{"nombre":"CABA","provincia":{"nombre":"Buenos Aires"}}},
                 "contactos":[{"tipo":"EMAIL","valor":"comedor@mail.com"}]}
                """;
        idEntidad = UUID.fromString(enviar(post("/entidades").contentType(MediaType.APPLICATION_JSON).content(entidad))
                .get("id").asText());
    }

    /** Una donación asignada a la entidad, llevada a mano hasta el estado indicado. */
    private UUID donacionEn(String... estados) throws Exception {
        String donacion = """
                {"idDonante":"%s","descripcion":"Colecta","bienes":[{"subcategoria":"Arroz","cantidad":5,"unidad":"kg"}]}
                """.formatted(idDonante);
        UUID id = UUID.fromString(enviar(post("/donaciones").contentType(MediaType.APPLICATION_JSON).content(donacion))
                .get(0).get("id").asText());
        mvc.perform(patch("/donaciones/{id}/asignar", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idEntidadBeneficiaria\":\"" + idEntidad + "\"}"))
                .andExpect(status().isNoContent());
        for (String estado : estados) {
            mvc.perform(patch("/donaciones/{id}/estado", id).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"estado\":\"" + estado + "\"}"))
                    .andExpect(status().isNoContent());
        }
        return id;
    }

    private String inicioDeRuta(UUID... ids) throws Exception {
        StringBuilder lista = new StringBuilder();
        for (UUID id : ids) {
            lista.append(lista.isEmpty() ? "" : ",").append('"').append(id).append('"');
        }
        return "{\"idRuta\":\"" + UUID.randomUUID() + "\",\"idsDonaciones\":[" + lista
                + "],\"urlMapaInteractivo\":\"https://donatrack.org/mapa/1\"}";
    }

    private Donacion leer(UUID id) {
        return donaciones.buscarPorId(id).orElseThrow();
    }

    /**
     * El historial se carga recién cuando se usa (LAZY) y fuera de una transacción
     * ya no hay sesión para cargarlo: se lee y se copia dentro de una, como hace
     * cualquier service.
     */
    private List<CambioEstado> historialDe(UUID idDonacion) {
        return transaccion.execute(estado ->
                List.copyOf(donaciones.buscarPorId(idDonacion).orElseThrow().getHistorialEstados()));
    }

    @Test
    @DisplayName("Si una donación de la ruta no puede salir, no sale ninguna (ni sus avisos)")
    void rutaTodoONada() throws Exception {
        UUID lista = donacionEn("LISTA_PARA_ENTREGAR");
        UUID sinPlanificar = donacionEn();
        long avisosAntes = mensajes.findByDestino(Destinos.NOTIFICACION).size();

        mvc.perform(post("/logistica/eventos/inicio-ruta").contentType(MediaType.APPLICATION_JSON)
                        .content(inicioDeRuta(lista, sinPlanificar)))
                .andExpect(status().isUnprocessableEntity());

        assertThat(leer(lista).estaEnEstado("LISTA_PARA_ENTREGAR")).isTrue();
        assertThat(mensajes.findByDestino(Destinos.NOTIFICACION)).hasSize((int) avisosAntes);
    }

    @Test
    @DisplayName("Un inicio de ruta repetido responde 200 y no duplica el historial ni los avisos")
    void inicioRepetido() throws Exception {
        UUID id = donacionEn("LISTA_PARA_ENTREGAR");
        String aviso = inicioDeRuta(id);

        mvc.perform(post("/logistica/eventos/inicio-ruta").contentType(MediaType.APPLICATION_JSON).content(aviso))
                .andExpect(status().isOk());
        long avisos = mensajes.findByDestino(Destinos.NOTIFICACION).size();
        mvc.perform(post("/logistica/eventos/inicio-ruta").contentType(MediaType.APPLICATION_JSON).content(aviso))
                .andExpect(status().isOk());

        assertThat(leer(id).estaEnEstado("EN_TRASLADO")).isTrue();
        assertThat(historialDe(id)).filteredOn(c -> c.getEstado().es("EN_TRASLADO")).hasSize(1);
        assertThat(mensajes.findByDestino(Destinos.NOTIFICACION)).hasSize((int) avisos);
    }

    @Test
    @DisplayName("Una entrega exitosa repetida responde 200 y no se registra dos veces")
    void entregaRepetida() throws Exception {
        UUID id = donacionEn("LISTA_PARA_ENTREGAR", "EN_TRASLADO");
        String aviso = "{\"idDonacion\":\"" + id + "\",\"idCamion\":\"" + UUID.randomUUID()
                + "\",\"patenteCamion\":\"AB123CD\",\"fechaHoraEntrega\":\"2026-03-15T14:30:00\"}";

        mvc.perform(post("/logistica/eventos/entrega-exitosa").contentType(MediaType.APPLICATION_JSON).content(aviso))
                .andExpect(status().isOk());
        mvc.perform(post("/logistica/eventos/entrega-exitosa").contentType(MediaType.APPLICATION_JSON).content(aviso))
                .andExpect(status().isOk());

        assertThat(historialDe(id)).filteredOn(c -> c.getEstado().es("ENTREGADA")).hasSize(1);
        assertThat(mensajes.findByDestino(Destinos.INCENTIVOS_DONACION_EXITOSA)).hasSize(1);
    }
}
