package ar.utn.donatrack.brokerlogistica.controllers;

import ar.utn.donatrack.brokerlogistica.dtos.EnvioAsignadoDTO;
import ar.utn.donatrack.brokerlogistica.dtos.ResultadoEnvioResponse;
import ar.utn.donatrack.brokerlogistica.exceptions.ProveedorDesconocidoException;
import ar.utn.donatrack.brokerlogistica.exceptions.SinProveedorDisponibleException;
import ar.utn.donatrack.brokerlogistica.services.BrokerEnviosService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EnviosBrokerController.class)
@DisplayName("EnviosBrokerController - contrato HTTP de /api/broker")
class EnviosBrokerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BrokerEnviosService brokerEnviosService;

    private final UUID idDonacion = UUID.randomUUID();

    private String solicitud() {
        return """
                {"donaciones": [{
                    "idDonacion": "%s",
                    "idEntidadBeneficiaria": "%s",
                    "direccionEntrega": {"calle": "Medrano", "numero": 951, "localidad": "CABA", "provincia": "Buenos Aires"}
                }]}
                """.formatted(idDonacion, UUID.randomUUID());
    }

    @Test
    @DisplayName("POST /envios responde 201 con el proveedor elegido")
    void solicitarOk() throws Exception {
        when(brokerEnviosService.solicitarEnvio(any())).thenReturn(new ResultadoEnvioResponse("EXTERNA", List.of("DONATRACK"),
                List.of(new EnvioAsignadoDTO(idDonacion, "EXTERNA", "EXT-1", LocalDateTime.now()))));

        mockMvc.perform(post("/api/broker/envios").contentType(MediaType.APPLICATION_JSON).content(solicitud()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proveedor").value("EXTERNA"))
                .andExpect(jsonPath("$.proveedoresDescartados[0]").value("DONATRACK"))
                .andExpect(jsonPath("$.envios[0].idSeguimiento").value("EXT-1"));
    }

    @Test
    @DisplayName("POST /envios sin donaciones responde 400")
    void solicitarSinDonaciones() throws Exception {
        mockMvc.perform(post("/api/broker/envios").contentType(MediaType.APPLICATION_JSON).content("{\"donaciones\": []}"))
                .andExpect(status().isBadRequest());

        verify(brokerEnviosService, never()).solicitarEnvio(any());
    }

    @Test
    @DisplayName("POST /envios con proveedor desconocido responde 400")
    void proveedorDesconocido() throws Exception {
        when(brokerEnviosService.solicitarEnvio(any()))
                .thenThrow(new ProveedorDesconocidoException("CORREO", List.of("DONATRACK", "EXTERNA")));

        mockMvc.perform(post("/api/broker/envios").contentType(MediaType.APPLICATION_JSON).content(solicitud()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /envios sin proveedores disponibles responde 503")
    void sinProveedores() throws Exception {
        when(brokerEnviosService.solicitarEnvio(any()))
                .thenThrow(new SinProveedorDisponibleException(List.of("DONATRACK", "EXTERNA")));

        mockMvc.perform(post("/api/broker/envios").contentType(MediaType.APPLICATION_JSON).content(solicitud()))
                .andExpect(status().isServiceUnavailable());
    }
}
