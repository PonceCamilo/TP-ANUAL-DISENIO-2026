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
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServiceUnavailable;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("DonatrackLogisticaAdapter - traducción al contrato de servicio-logistica")
class DonatrackLogisticaAdapterTest {

    private static final String BASE = "http://logistica";

    private MockRestServiceServer servidor;
    private DonatrackLogisticaAdapter adapter;

    @BeforeEach
    void prepararEscenario() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        adapter = new DonatrackLogisticaAdapter(builder, BASE);
    }

    private DonacionEnvioDTO donacion() {
        return new DonacionEnvioDTO(UUID.randomUUID(), UUID.randomUUID(), "Comedor",
                new DireccionDTO("Medrano", 951, "CABA", "Buenos Aires", "C1179"));
    }

    @Test
    @DisplayName("Manda todas las donaciones en una planificación sin camiones y asigna cada una a su lote")
    void planificaYAsignaLotes() {
        DonacionEnvioDTO d1 = donacion();
        DonacionEnvioDTO d2 = donacion();
        DonacionEnvioDTO d3 = donacion();
        UUID lote1 = UUID.randomUUID();
        UUID lote2 = UUID.randomUUID();

        servidor.expect(requestTo(BASE + "/api/logistica/planificaciones"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.donaciones.length()").value(3))
                .andExpect(jsonPath("$.donaciones[0].idDonacion").value(d1.idDonacion().toString()))
                .andExpect(jsonPath("$.donaciones[0].direccionEntrega.calle").value("Medrano"))
                .andExpect(jsonPath("$.camionesIds").doesNotExist())
                .andRespond(withSuccess("""
                        [{"id":"%s","cantidadDonaciones":2,"estado":"COMPLETADO"},
                         {"id":"%s","cantidadDonaciones":1,"estado":"COMPLETADO"}]
                        """.formatted(lote1, lote2), MediaType.APPLICATION_JSON));

        List<EnvioAsignadoDTO> envios = adapter.solicitarEnvios(List.of(d1, d2, d3));

        assertThat(envios).extracting(EnvioAsignadoDTO::idSeguimiento)
                .containsExactly(lote1.toString(), lote1.toString(), lote2.toString());
        assertThat(envios).extracting(EnvioAsignadoDTO::proveedor).containsOnly("DONATRACK");
        servidor.verify();
    }

    @Test
    @DisplayName("Un 503 de logística se traduce a ProveedorNoDisponibleException")
    void proveedorCaido() {
        servidor.expect(requestTo(BASE + "/api/logistica/planificaciones"))
                .andRespond(withServiceUnavailable());

        assertThatThrownBy(() -> adapter.solicitarEnvios(List.of(donacion())))
                .isInstanceOf(ProveedorNoDisponibleException.class);
    }

    @Test
    @DisplayName("El health check lee el status de /actuator/health")
    void healthCheck() {
        servidor.expect(requestTo(BASE + "/actuator/health"))
                .andRespond(withSuccess("{\"status\":\"UP\"}", MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(BASE + "/actuator/health"))
                .andRespond(withServiceUnavailable());

        assertThat(adapter.disponible()).isTrue();
        assertThat(adapter.disponible()).isFalse();
    }
}
