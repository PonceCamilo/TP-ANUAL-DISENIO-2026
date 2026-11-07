package ar.utn.donatrack.brokerlogistica.adapters;

import ar.utn.donatrack.brokerlogistica.dtos.DireccionDTO;
import ar.utn.donatrack.brokerlogistica.dtos.DonacionEnvioDTO;
import ar.utn.donatrack.brokerlogistica.dtos.EnvioAsignadoDTO;
import ar.utn.donatrack.brokerlogistica.exceptions.ProveedorNoDisponibleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@DisplayName("ExternaLogisticaAdapter - traducción al contrato del proveedor externo")
class ExternaLogisticaAdapterTest {

    private static final String BASE = "http://externa";

    private MockRestServiceServer servidor;
    private ExternaLogisticaAdapter adapter;

    @BeforeEach
    void prepararEscenario() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        adapter = new ExternaLogisticaAdapter(builder, BASE);
    }

    private String creado(String trackingId) {
        return "{\"trackingId\":\"" + trackingId + "\",\"estado\":\"ACEPTADO\"}";
    }

    @Test
    @DisplayName("Crea un envío por donación con shipmentRef, destinatario y domicilio en una línea")
    void unEnvioPorDonacion() {
        DonacionEnvioDTO conNombre = new DonacionEnvioDTO(UUID.randomUUID(), UUID.randomUUID(), "Comedor Los Pibes",
                new DireccionDTO("Medrano", 951, "CABA", "Buenos Aires", "C1179"));
        UUID idEntidad = UUID.randomUUID();
        DonacionEnvioDTO sinNombre = new DonacionEnvioDTO(UUID.randomUUID(), idEntidad, null,
                new DireccionDTO("Mozart", 2300, "CABA", "Buenos Aires", null));

        servidor.expect(requestTo(BASE + "/api/v1/envios"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.shipmentRef").value(conNombre.idDonacion().toString()))
                .andExpect(jsonPath("$.destinatario").value("Comedor Los Pibes"))
                .andExpect(jsonPath("$.domicilio").value("Medrano 951, CABA, Buenos Aires, C1179"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON).body(creado("EXT-A")));
        servidor.expect(requestTo(BASE + "/api/v1/envios"))
                .andExpect(jsonPath("$.destinatario").value("Entidad beneficiaria " + idEntidad))
                .andExpect(jsonPath("$.domicilio").value("Mozart 2300, CABA, Buenos Aires"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON).body(creado("EXT-B")));

        List<EnvioAsignadoDTO> envios = adapter.solicitarEnvios(List.of(conNombre, sinNombre));

        assertThat(envios).extracting(EnvioAsignadoDTO::idSeguimiento).containsExactly("EXT-A", "EXT-B");
        assertThat(envios).extracting(EnvioAsignadoDTO::proveedor).containsOnly("EXTERNA");
        servidor.verify();
    }

    @Test
    @DisplayName("Un error del proveedor se traduce a ProveedorNoDisponibleException")
    void proveedorConError() {
        servidor.expect(requestTo(BASE + "/api/v1/envios")).andRespond(withServerError());

        DonacionEnvioDTO donacion = new DonacionEnvioDTO(UUID.randomUUID(), UUID.randomUUID(), "X",
                new DireccionDTO("Medrano", 951, "CABA", "Buenos Aires", null));

        assertThatThrownBy(() -> adapter.solicitarEnvios(List.of(donacion)))
                .isInstanceOf(ProveedorNoDisponibleException.class);
    }
}
