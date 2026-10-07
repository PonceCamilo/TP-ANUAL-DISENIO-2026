package ar.utn.donatrack.logisticaexterna.controllers;

import ar.utn.donatrack.logisticaexterna.dtos.EnvioResponse;
import ar.utn.donatrack.logisticaexterna.exceptions.EnvioNoEncontradoException;
import ar.utn.donatrack.logisticaexterna.exceptions.TransicionEnvioIlegalException;
import ar.utn.donatrack.logisticaexterna.models.EstadoEnvio;
import ar.utn.donatrack.logisticaexterna.services.EnvioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EnviosController.class)
@DisplayName("EnviosController - contrato HTTP de /api/v1/envios")
class EnviosControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnvioService envioService;

    private EnvioResponse envio(String estado) {
        return new EnvioResponse("EXT-1", "don-123", "Comedor", "Medrano 951", estado,
                null, null, "http://externo/envios/EXT-1", LocalDate.now(), List.of());
    }

    @Test
    @DisplayName("POST crea el envío y responde 201")
    void crear() throws Exception {
        when(envioService.crear(any())).thenReturn(envio("ACEPTADO"));

        mockMvc.perform(post("/api/v1/envios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"shipmentRef\":\"don-123\",\"destinatario\":\"Comedor\",\"domicilio\":\"Medrano 951\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.trackingId").value("EXT-1"))
                .andExpect(jsonPath("$.estado").value("ACEPTADO"));
    }

    @Test
    @DisplayName("POST sin domicilio responde 400 y no llama al service")
    void crearInvalido() throws Exception {
        mockMvc.perform(post("/api/v1/envios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"shipmentRef\":\"don-123\",\"destinatario\":\"Comedor\"}"))
                .andExpect(status().isBadRequest());

        verify(envioService, never()).crear(any());
    }

    @Test
    @DisplayName("GET de un tracking inexistente responde 404")
    void consultar404() throws Exception {
        when(envioService.consultar("EXT-X")).thenThrow(new EnvioNoEncontradoException("EXT-X"));

        mockMvc.perform(get("/api/v1/envios/{id}", "EXT-X"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Despachar responde 200 con el envío EN_CAMINO")
    void despachar() throws Exception {
        when(envioService.despachar("EXT-1", "AB123CD")).thenReturn(envio("EN_CAMINO"));

        mockMvc.perform(post("/api/v1/envios/{id}/despacho", "EXT-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehiculo\":\"AB123CD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_CAMINO"));
    }

    @Test
    @DisplayName("Una transición ilegal responde 409")
    void transicionIlegal() throws Exception {
        when(envioService.entregar("EXT-1"))
                .thenThrow(new TransicionEnvioIlegalException("EXT-1", EstadoEnvio.ACEPTADO, EstadoEnvio.ENTREGADO));

        mockMvc.perform(post("/api/v1/envios/{id}/entrega", "EXT-1"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Fallo sin motivo responde 400")
    void falloSinMotivo() throws Exception {
        mockMvc.perform(post("/api/v1/envios/{id}/fallo", "EXT-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
