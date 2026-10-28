package ar.utn.donatrack.donaciones.controllers;

import ar.utn.donatrack.donaciones.exceptions.cambioEstadosExceptions.TransicionDeEstadoIlegalException;
import ar.utn.donatrack.donaciones.exceptions.donacionesExceptions.DonacionNoEncontradaException;
import ar.utn.donatrack.donaciones.services.logistica.ManejadorEntregaExitosa;
import ar.utn.donatrack.donaciones.services.logistica.ManejadorEntregaFallida;
import ar.utn.donatrack.donaciones.services.logistica.ManejadorInicioDeRuta;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests del endpoint que recibe los eventos del Servicio de Logística.
 *
 * Es la puerta de entrada del contrato entre microservicios: logística publica
 * acá lo que le pasa a cada donación (inicio de ruta, entrega exitosa, entrega
 * fallida) y donaciones se encarga de actualizar el estado y notificar.
 *
 * Como el emisor es otro servicio y no un formulario, la validación del cuerpo
 * es especialmente importante: un payload incompleto tiene que rechazarse con
 * 400 en vez de procesarse a medias.
 */
@WebMvcTest(LogisticaEventosController.class)
@DisplayName("LogisticaEventosController - contrato HTTP de /logistica/eventos")
class LogisticaEventosControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ManejadorInicioDeRuta inicioDeRuta;

    @MockitoBean
    private ManejadorEntregaExitosa entregaExitosa;

    @MockitoBean
    private ManejadorEntregaFallida entregaFallida;

    private final UUID idDonacion = UUID.randomUUID();

    @Nested
    @DisplayName("POST /logistica/eventos/inicio-ruta")
    class InicioRuta {

        private String cuerpoValido() throws Exception {
            return objectMapper.writeValueAsString(Map.of(
                    "idRuta", UUID.randomUUID(),
                    "idsDonaciones", List.of(idDonacion),
                    "urlMapaInteractivo", "https://donatrack.org/mapa/ruta-123"));
        }

        @Test
        @DisplayName("Devuelve 200 y delega el evento en el service")
        void inicioRutaOk() throws Exception {
            mockMvc.perform(post("/logistica/eventos/inicio-ruta")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoValido()))
                    .andExpect(status().isOk());

            verify(inicioDeRuta).procesar(any());
        }

        @Test
        @DisplayName("Devuelve 400 si la lista de donaciones viene vacía")
        void sinDonaciones() throws Exception {
            // @NotEmpty: un inicio de ruta sin donaciones no tiene sentido y
            // conviene detectarlo acá antes de tocar nada.
            String cuerpo = objectMapper.writeValueAsString(Map.of(
                    "idRuta", UUID.randomUUID(),
                    "idsDonaciones", List.of(),
                    "urlMapaInteractivo", "https://donatrack.org/mapa/ruta-123"));

            mockMvc.perform(post("/logistica/eventos/inicio-ruta")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isBadRequest());

            verify(inicioDeRuta, never()).procesar(any());
        }

        @Test
        @DisplayName("Devuelve 400 si falta el id de la ruta")
        void sinIdRuta() throws Exception {
            String cuerpo = objectMapper.writeValueAsString(Map.of(
                    "idsDonaciones", List.of(idDonacion),
                    "urlMapaInteractivo", "https://donatrack.org/mapa/ruta-123"));

            mockMvc.perform(post("/logistica/eventos/inicio-ruta")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Devuelve 404 si alguna donación del evento no existe")
        void donacionInexistente() throws Exception {
            doThrow(new DonacionNoEncontradaException(idDonacion))
                    .when(inicioDeRuta).procesar(any());

            mockMvc.perform(post("/logistica/eventos/inicio-ruta")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoValido()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Devuelve 422 si la donación no está en un estado que permita iniciar la ruta")
        void estadoIncompatible() throws Exception {
            doThrow(new TransicionDeEstadoIlegalException("EN_DEPOSITO", "EN_TRASLADO"))
                    .when(inicioDeRuta).procesar(any());

            mockMvc.perform(post("/logistica/eventos/inicio-ruta")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoValido()))
                    .andExpect(status().isUnprocessableEntity());
        }
    }

    @Nested
    @DisplayName("POST /logistica/eventos/entrega-exitosa")
    class EntregaExitosa {

        private String cuerpoValido() throws Exception {
            return objectMapper.writeValueAsString(Map.of(
                    "idDonacion", idDonacion,
                    "idCamion", UUID.randomUUID(),
                    "patenteCamion", "AB123CD",
                    "fechaHoraEntrega", "2026-03-15T14:30:00"));
        }

        @Test
        @DisplayName("Devuelve 200 y delega el evento en el service")
        void entregaExitosaOk() throws Exception {
            mockMvc.perform(post("/logistica/eventos/entrega-exitosa")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoValido()))
                    .andExpect(status().isOk());

            verify(entregaExitosa).procesar(any());
        }

        @Test
        @DisplayName("Devuelve 400 si falta la patente del camión")
        void faltaLaPatente() throws Exception {
            // La patente es parte del comprobante de entrega que exige el enunciado.
            String cuerpo = objectMapper.writeValueAsString(Map.of(
                    "idDonacion", idDonacion,
                    "idCamion", UUID.randomUUID(),
                    "fechaHoraEntrega", "2026-03-15T14:30:00"));

            mockMvc.perform(post("/logistica/eventos/entrega-exitosa")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Devuelve 400 si la fecha de entrega tiene un formato inválido")
        void fechaMalFormada() throws Exception {
            String cuerpo = objectMapper.writeValueAsString(Map.of(
                    "idDonacion", idDonacion,
                    "idCamion", UUID.randomUUID(),
                    "patenteCamion", "AB123CD",
                    "fechaHoraEntrega", "15/03/2026"));

            mockMvc.perform(post("/logistica/eventos/entrega-exitosa")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Devuelve 404 si la donación no existe")
        void donacionInexistente() throws Exception {
            doThrow(new DonacionNoEncontradaException(idDonacion))
                    .when(entregaExitosa).procesar(any());

            mockMvc.perform(post("/logistica/eventos/entrega-exitosa")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoValido()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /logistica/eventos/entrega-fallida")
    class EntregaFallida {

        private String cuerpoValido() throws Exception {
            return objectMapper.writeValueAsString(Map.of(
                    "idDonacion", idDonacion,
                    "motivoFallo", "Tocamos timbre pero nadie respondió",
                    "replanificable", true));
        }

        @Test
        @DisplayName("Devuelve 200 y delega el evento en el service")
        void entregaFallidaOk() throws Exception {
            mockMvc.perform(post("/logistica/eventos/entrega-fallida")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoValido()))
                    .andExpect(status().isOk());

            verify(entregaFallida).procesar(any());
        }

        @Test
        @DisplayName("Devuelve 400 si no se informa el motivo del fallo")
        void sinMotivo() throws Exception {
            // El motivo es obligatorio: sin él, la máquina de estados rechazaría
            // la transición y además se perdería la trazabilidad que pide el enunciado.
            String cuerpo = objectMapper.writeValueAsString(Map.of(
                    "idDonacion", idDonacion,
                    "replanificable", true));

            mockMvc.perform(post("/logistica/eventos/entrega-fallida")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isBadRequest());

            verify(entregaFallida, never()).procesar(any());
        }

        @Test
        @DisplayName("Devuelve 400 si el motivo viene en blanco")
        void motivoEnBlanco() throws Exception {
            String cuerpo = objectMapper.writeValueAsString(Map.of(
                    "idDonacion", idDonacion,
                    "motivoFallo", "   ",
                    "replanificable", false));

            mockMvc.perform(post("/logistica/eventos/entrega-fallida")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Devuelve 422 si la donación no está EN_TRASLADO")
        void estadoIncompatible() throws Exception {
            doThrow(new TransicionDeEstadoIlegalException("EN_DEPOSITO", "ENTREGA_FALLIDA"))
                    .when(entregaFallida).procesar(any());

            mockMvc.perform(post("/logistica/eventos/entrega-fallida")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoValido()))
                    .andExpect(status().isUnprocessableEntity());
        }
    }
}
