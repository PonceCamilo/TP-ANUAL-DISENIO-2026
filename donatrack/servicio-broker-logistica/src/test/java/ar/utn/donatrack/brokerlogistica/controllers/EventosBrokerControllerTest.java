package ar.utn.donatrack.brokerlogistica.controllers;

import ar.utn.donatrack.brokerlogistica.exceptions.DonacionesNoDisponibleException;
import ar.utn.donatrack.brokerlogistica.services.BrokerEventosService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventosBrokerController.class)
@DisplayName("EventosBrokerController - webhooks de los proveedores")
class EventosBrokerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BrokerEventosService brokerEventosService;

    @Test
    @DisplayName("POST /eventos/donatrack responde 200")
    void eventoDonatrack() throws Exception {
        mockMvc.perform(post("/api/broker/eventos/donatrack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"ENTREGA_CONFIRMADA\",\"idDonacion\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isOk());

        verify(brokerEventosService).procesarEventoDonatrack(any());
    }

    @Test
    @DisplayName("Si Donaciones no acepta el evento responde 502")
    void donacionesCaido() throws Exception {
        doThrow(new DonacionesNoDisponibleException("/logistica/eventos/inicio-ruta", new RuntimeException("Connection refused")))
                .when(brokerEventosService).procesarEventoDonatrack(any());

        mockMvc.perform(post("/api/broker/eventos/donatrack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"INICIO_RUTA\"}"))
                .andExpect(status().isBadGateway());
    }

    @Test
    @DisplayName("POST /eventos/externa responde 200 indicando si se reenvió")
    void eventoExterno() throws Exception {
        when(brokerEventosService.procesarEventoExterno(any())).thenReturn(true);

        mockMvc.perform(post("/api/broker/eventos/externa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trackingId\":\"EXT-1\",\"shipmentRef\":\"" + UUID.randomUUID() + "\",\"status\":\"ENTREGADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reenviado").value(true));
    }

    @Test
    @DisplayName("POST /eventos/externa sin status responde 400 y no procesa")
    void eventoExternoInvalido() throws Exception {
        mockMvc.perform(post("/api/broker/eventos/externa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trackingId\":\"EXT-1\",\"shipmentRef\":\"x\"}"))
                .andExpect(status().isBadRequest());

        verify(brokerEventosService, never()).procesarEventoExterno(any());
    }
}
