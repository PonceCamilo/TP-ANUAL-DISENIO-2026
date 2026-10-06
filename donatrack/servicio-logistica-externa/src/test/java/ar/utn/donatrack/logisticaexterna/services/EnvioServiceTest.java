package ar.utn.donatrack.logisticaexterna.services;

import ar.utn.donatrack.logisticaexterna.dtos.CrearEnvioRequest;
import ar.utn.donatrack.logisticaexterna.dtos.EnvioResponse;
import ar.utn.donatrack.logisticaexterna.dtos.EventoEnvio;
import ar.utn.donatrack.logisticaexterna.exceptions.EnvioNoEncontradoException;
import ar.utn.donatrack.logisticaexterna.exceptions.TransicionEnvioIlegalException;
import ar.utn.donatrack.logisticaexterna.integracion.WebhookEventosNotifier;
import ar.utn.donatrack.logisticaexterna.repositories.EnvioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * El repositorio es en memoria y trivial, así que se usa real; solo se
 * mockea el webhook para verificar qué eventos se avisan.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EnvioService - alta, tracking y eventos")
class EnvioServiceTest {

    private static final String BASE_URL = "http://externo/envios/";

    @Mock
    private WebhookEventosNotifier notifier;

    @Captor
    private ArgumentCaptor<EventoEnvio> eventoCaptor;

    private EnvioService servicio;

    @BeforeEach
    void prepararEscenario() {
        servicio = new EnvioService(new EnvioRepository(), notifier, BASE_URL, 2);
    }

    private EnvioResponse crearEnvio() {
        return servicio.crear(new CrearEnvioRequest("don-123", "Comedor Los Pibes", "Medrano 951, CABA"));
    }

    @Test
    @DisplayName("El alta devuelve tracking, estado ACEPTADO y fecha estimada, sin avisar por webhook")
    void alta() {
        EnvioResponse envio = crearEnvio();

        assertThat(envio.trackingId()).startsWith("EXT-");
        assertThat(envio.shipmentRef()).isEqualTo("don-123");
        assertThat(envio.estado()).isEqualTo("ACEPTADO");
        assertThat(envio.trackingUrl()).isEqualTo(BASE_URL + envio.trackingId());
        assertThat(envio.fechaEstimadaEntrega()).isEqualTo(LocalDate.now().plusDays(2));
        assertThat(servicio.consultar(envio.trackingId()).shipmentRef()).isEqualTo("don-123");
        verifyNoInteractions(notifier);
    }

    @Test
    @DisplayName("Despachar avisa EN_CAMINO con vehículo y link de seguimiento")
    void despacharAvisaEnCamino() {
        EnvioResponse envio = crearEnvio();

        servicio.despachar(envio.trackingId(), "AB123CD");

        verify(notifier).notificar(eventoCaptor.capture());
        EventoEnvio evento = eventoCaptor.getValue();
        assertThat(evento.status()).isEqualTo("EN_CAMINO");
        assertThat(evento.shipmentRef()).isEqualTo("don-123");
        assertThat(evento.vehiculo()).isEqualTo("AB123CD");
        assertThat(evento.trackingUrl()).isEqualTo(BASE_URL + envio.trackingId());
    }

    @Test
    @DisplayName("Entregar y fallar avisan su estado; el fallo incluye el motivo")
    void entregaYFalloAvisan() {
        EnvioResponse exitoso = crearEnvio();
        servicio.despachar(exitoso.trackingId(), "AB123CD");
        servicio.entregar(exitoso.trackingId());

        EnvioResponse fallido = crearEnvio();
        servicio.despachar(fallido.trackingId(), "AB123CD");
        servicio.fallar(fallido.trackingId(), "Destinatario ausente");

        verify(notifier, times(4)).notificar(eventoCaptor.capture());
        assertThat(eventoCaptor.getAllValues()).extracting(EventoEnvio::status)
                .containsExactly("EN_CAMINO", "ENTREGADO", "EN_CAMINO", "FALLIDO");
        assertThat(eventoCaptor.getAllValues().get(3).motivo()).isEqualTo("Destinatario ausente");
    }

    @Test
    @DisplayName("Una transición ilegal no avisa nada")
    void transicionIlegalNoAvisa() {
        EnvioResponse envio = crearEnvio();

        assertThatThrownBy(() -> servicio.entregar(envio.trackingId()))
                .isInstanceOf(TransicionEnvioIlegalException.class);
        verify(notifier, never()).notificar(any());
    }

    @Test
    @DisplayName("Tracking inexistente lanza EnvioNoEncontradoException")
    void trackingInexistente() {
        assertThatThrownBy(() -> servicio.consultar("EXT-NOEXISTE"))
                .isInstanceOf(EnvioNoEncontradoException.class);
    }
}
