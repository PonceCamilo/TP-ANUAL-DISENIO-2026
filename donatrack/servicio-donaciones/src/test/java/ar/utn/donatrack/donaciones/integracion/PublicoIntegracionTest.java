package ar.utn.donatrack.donaciones.integracion;

import ar.utn.donatrack.donaciones.seguridad.SeguridadDePrueba;
import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Los datos públicos (Entrega 6) por la API y contra la base: una donación que
 * recorre el camino real hasta ENTREGADA (con las fotos que manda logística)
 * aparece en la landing sin token, con la entidad y sus coordenadas, y sin ningún
 * dato de la persona donante ni de los contactos de la entidad.
 *
 * Los datos se cargan como administración (SeguridadDePrueba); las consultas
 * públicas van sin token (anonymous()).
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:publico_integracion;DB_CLOSE_DELAY=-1;MODE=PostgreSQL")
@AutoConfigureMockMvc
@Import(SeguridadDePrueba.class)
@DisplayName("Integración: datos públicos para la landing, el mapa y las entidades")
class PublicoIntegracionTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;

    private String emailDonante;
    private UUID idDonante;
    private UUID idEntidad;

    private JsonNode enviar(RequestBuilder pedido) throws Exception {
        String cuerpo = mvc.perform(pedido).andReturn().getResponse().getContentAsString();
        return cuerpo.isBlank() ? null : json.readTree(cuerpo);
    }

    private JsonNode publico(String ruta) throws Exception {
        String cuerpo = mvc.perform(get(ruta).with(anonymous()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(cuerpo);
    }

    @BeforeEach
    void crearDonanteYEntidad() throws Exception {
        emailDonante = "ana" + UUID.randomUUID() + "@mail.com";
        String donante = """
                {"tipo":"HUMANA","tipoDocumento":"DNI","numeroDocumento":"30111222","email":"%s",
                 "nombre":"Ana","apellido":"Pérez","fechaNacimiento":"1990-05-10","genero":"FEMENINO",
                 "direccion":{"calle":"Medrano","numero":951,"codigoPostal":"1179",
                              "localidad":{"nombre":"CABA","provincia":{"nombre":"Buenos Aires"}}},
                 "medioContactoPredeterminado":{"tipo":"EMAIL","valor":"%s"}}
                """.formatted(emailDonante, emailDonante);
        idDonante = UUID.fromString(enviar(post("/donantes").contentType(MediaType.APPLICATION_JSON).content(donante))
                .get("id").asText());
        String entidad = """
                {"razonSocial":"Comedor Público %s",
                 "direccion":{"calle":"Medrano","numero":951,"codigoPostal":"1179",
                              "localidad":{"nombre":"CABA","provincia":{"nombre":"Buenos Aires"}},
                              "latitud":-34.5986,"longitud":-58.4202},
                 "contactos":[{"tipo":"EMAIL","valor":"contacto-privado@comedor.org"}]}
                """.formatted(UUID.randomUUID());
        idEntidad = UUID.fromString(enviar(post("/entidades").contentType(MediaType.APPLICATION_JSON).content(entidad))
                .get("id").asText());
    }

    /** Una donación con foto, asignada, despachada y entregada con las fotos de la recepción. */
    private UUID donacionEntregada() throws Exception {
        String donacion = """
                {"idDonante":"%s","descripcion":"Colecta de Ana en su casa","bienes":[
                  {"subcategoria":"Arroz","descripcion":"Arroz de Ana","cantidad":5,"unidad":"kg",
                   "foto":"https://fotos.org/bien.jpg"}]}
                """.formatted(idDonante);
        UUID id = UUID.fromString(enviar(post("/donaciones").contentType(MediaType.APPLICATION_JSON).content(donacion))
                .get(0).get("id").asText());
        mvc.perform(patch("/donaciones/{id}/asignar", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idEntidadBeneficiaria\":\"" + idEntidad + "\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(patch("/donaciones/{id}/estado", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"LISTA_PARA_ENTREGAR\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(post("/logistica/eventos/inicio-ruta").with(anonymous()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idRuta\":\"" + UUID.randomUUID() + "\",\"idsDonaciones\":[\"" + id
                                + "\"],\"urlMapaInteractivo\":\"https://donatrack.org/mapa/1\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/logistica/eventos/entrega-exitosa").with(anonymous()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idDonacion":"%s","idCamion":"%s","patenteCamion":"AB123CD",
                                 "fechaHoraEntrega":"%sT10:00:00",
                                 "fotos":["https://fotos.org/recepcion-1.jpg","https://fotos.org/recepcion-2.jpg"]}
                                """.formatted(id, UUID.randomUUID(), FechaHoraArgentina.hoy())))
                .andExpect(status().isOk());
        return id;
    }

    @Test
    @DisplayName("La entregada aparece en la landing sin token, con la entidad, sus coordenadas y la foto de la recepción")
    void entregadaEnLaLanding() throws Exception {
        UUID id = donacionEntregada();

        JsonNode pagina = publico("/publico/donaciones-entregadas?size=100");
        JsonNode encontrada = null;
        for (JsonNode donacion : pagina.get("contenido")) {
            if (donacion.get("id").asText().equals(id.toString())) {
                encontrada = donacion;
            }
        }

        assertThat(encontrada).isNotNull();
        assertThat(pagina.get("pagina").asInt()).isEqualTo(1);
        assertThat(encontrada.get("subcategoria").asText()).isEqualTo("Arroz");
        assertThat(encontrada.get("cantidad").asInt()).isEqualTo(5);
        assertThat(encontrada.get("foto").asText()).isEqualTo("https://fotos.org/recepcion-1.jpg");
        assertThat(encontrada.get("entidad").get("idEntidad").asText()).isEqualTo(idEntidad.toString());
        assertThat(encontrada.get("entidad").get("latitud").asDouble()).isEqualTo(-34.5986);
        assertThat(encontrada.get("entidad").get("localidad").asText()).isEqualTo("CABA");
    }

    @Test
    @DisplayName("La landing no publica datos de la persona donante ni de los contactos de la entidad")
    void sinDatosPersonales() throws Exception {
        donacionEntregada();

        String landing = mvc.perform(get("/publico/donaciones-entregadas?size=100").with(anonymous()))
                .andReturn().getResponse().getContentAsString();
        String entidades = mvc.perform(get("/publico/entidades?size=100").with(anonymous()))
                .andReturn().getResponse().getContentAsString();

        assertThat(landing).doesNotContain(idDonante.toString(), emailDonante, "Colecta de Ana", "Arroz de Ana",
                "idDonante", "Medrano");
        assertThat(entidades).doesNotContain("contacto-privado@comedor.org", "Medrano", "representantes", "contactos");
    }

    @Test
    @DisplayName("Una entidad se ve sin token con sus necesidades abiertas de campañas vigentes")
    void entidadConNecesidades() throws Exception {
        LocalDate hoy = FechaHoraArgentina.hoy();
        UUID campania = UUID.fromString(enviar(post("/entidades/{id}/campanias", idEntidad)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"descripcionGeneral\":\"Invierno\",\"fechaInicio\":\"" + hoy
                        + "\",\"fechaFin\":\"" + hoy.plusMonths(1) + "\"}"))
                .get("idCampania").asText());
        mvc.perform(post("/entidades/{e}/campanias/{c}/necesidades", idEntidad, campania)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"EXTRAORDINARIA\",\"nombre\":\"Frazadas\",\"subcategoria\":\"Abrigo\","
                                + "\"cantidadObjetivo\":30}"))
                .andExpect(status().isCreated());

        JsonNode entidad = publico("/publico/entidades/" + idEntidad);

        assertThat(entidad.get("ubicacion").get("razonSocial").asText()).startsWith("Comedor Público");
        assertThat(entidad.get("necesidadesAbiertas")).hasSize(1);
        assertThat(entidad.get("necesidadesAbiertas").get(0).get("nombre").asText()).isEqualTo("Frazadas");
        assertThat(entidad.get("necesidadesAbiertas").get(0).get("campania").asText()).isEqualTo("Invierno");
    }

    @Test
    @DisplayName("El catálogo de subcategorías junta las de donaciones y necesidades, sin token")
    void catalogo() throws Exception {
        donacionEntregada();

        JsonNode catalogo = publico("/publico/subcategorias");

        assertThat(catalogo.isArray()).isTrue();
        assertThat(catalogo.toString()).contains("Arroz");
    }

    @Test
    @DisplayName("Un rango invertido es un error de quien llama (400)")
    void rangoInvertido() throws Exception {
        mvc.perform(get("/publico/donaciones-entregadas?desde=2026-11-10&hasta=2026-10-10").with(anonymous()))
                .andExpect(status().isBadRequest());
    }
}
