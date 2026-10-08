package ar.utn.donatrack.brokerlogistica.services;

import ar.utn.donatrack.brokerlogistica.clientes.DonacionesClient;
import ar.utn.donatrack.brokerlogistica.dtos.EventoExternoDTO;
import ar.utn.donatrack.brokerlogistica.dtos.donaciones.EntregaExitosaCallback;
import ar.utn.donatrack.brokerlogistica.dtos.donaciones.EntregaFallidaCallback;
import ar.utn.donatrack.brokerlogistica.dtos.donaciones.InicioRutaCallback;
import ar.utn.donatrack.brokerlogistica.exceptions.EventoInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("BrokerEventosService - traducción de eventos hacia Donaciones")
class BrokerEventosServiceTest {

    @Mock
    private DonacionesClient donacionesClient;

    @InjectMocks
    private BrokerEventosService servicio;

    @Nested
    @DisplayName("Eventos de servicio-logistica (DONATRACK)")
    class Donatrack {

        @Test
        @DisplayName("Enruta cada tipo al callback de Donaciones y reenvía el body tal cual")
        void enrutaPorTipo() {
            Map<String, Object> inicio = Map.of("tipo", "INICIO_RUTA", "idRuta", "r1");
            Map<String, Object> exitosa = Map.of("tipo", "ENTREGA_CONFIRMADA", "idDonacion", "d1");
            Map<String, Object> fallida = Map.of("tipo", "ENTREGA_NO_RECIBIDA", "idDonacion", "d1");

            servicio.procesarEventoDonatrack(inicio);
            servicio.procesarEventoDonatrack(exitosa);
            servicio.procesarEventoDonatrack(fallida);

            verify(donacionesClient).reenviar(DonacionesClient.INICIO_RUTA, inicio);
            verify(donacionesClient).reenviar(DonacionesClient.ENTREGA_EXITOSA, exitosa);
            verify(donacionesClient).reenviar(DonacionesClient.ENTREGA_FALLIDA, fallida);
        }

        @Test
        @DisplayName("Un tipo desconocido lanza EventoInvalidoException y no reenvía")
        void tipoDesconocido() {
            assertThatThrownBy(() -> servicio.procesarEventoDonatrack(Map.of("tipo", "OTRO")))
                    .isInstanceOf(EventoInvalidoException.class);
            verifyNoInteractions(donacionesClient);
        }
    }

    @Nested
    @DisplayName("Eventos de servicio-logistica-externa (EXTERNA)")
    class Externa {

        private final UUID idDonacion = UUID.randomUUID();
        private final LocalDateTime cuando = LocalDateTime.of(2026, 10, 6, 15, 30);

        private EventoExternoDTO evento(String status, String vehiculo, String motivo) {
            return new EventoExternoDTO("EXT-1", idDonacion.toString(), status, vehiculo, motivo,
                    "http://externo/envios/EXT-1", cuando);
        }

        @Test
        @DisplayName("EN_CAMINO se traduce a inicio de ruta con el link de tracking")
        void enCamino() {
            assertThat(servicio.procesarEventoExterno(evento("EN_CAMINO", "AB123CD", null))).isTrue();

            ArgumentCaptor<Object> body = ArgumentCaptor.forClass(Object.class);
            verify(donacionesClient).reenviar(eq(DonacionesClient.INICIO_RUTA), body.capture());
            InicioRutaCallback callback = (InicioRutaCallback) body.getValue();
            assertThat(callback.idsDonaciones()).containsExactly(idDonacion);
            assertThat(callback.urlMapaInteractivo()).isEqualTo("http://externo/envios/EXT-1");
            assertThat(callback.idRuta()).isNotNull();
        }

        @Test
        @DisplayName("ENTREGADO se traduce a entrega exitosa con patente, fecha e id de camión estable")
        void entregado() {
            servicio.procesarEventoExterno(evento("ENTREGADO", "AB123CD", null));
            servicio.procesarEventoExterno(evento("ENTREGADO", "AB123CD", null));

            ArgumentCaptor<Object> body = ArgumentCaptor.forClass(Object.class);
            verify(donacionesClient, org.mockito.Mockito.times(2)).reenviar(eq(DonacionesClient.ENTREGA_EXITOSA), body.capture());
            EntregaExitosaCallback primero = (EntregaExitosaCallback) body.getAllValues().get(0);
            EntregaExitosaCallback segundo = (EntregaExitosaCallback) body.getAllValues().get(1);
            assertThat(primero.idDonacion()).isEqualTo(idDonacion);
            assertThat(primero.patenteCamion()).isEqualTo("AB123CD");
            assertThat(primero.fechaHoraEntrega()).isEqualTo(cuando);
            assertThat(primero.idCamion()).isNotNull().isEqualTo(segundo.idCamion());
        }

        @Test
        @DisplayName("FALLIDO se traduce a entrega fallida replanificable con el motivo")
        void fallido() {
            servicio.procesarEventoExterno(evento("FALLIDO", "AB123CD", "Destinatario ausente"));

            ArgumentCaptor<Object> body = ArgumentCaptor.forClass(Object.class);
            verify(donacionesClient).reenviar(eq(DonacionesClient.ENTREGA_FALLIDA), body.capture());
            EntregaFallidaCallback callback = (EntregaFallidaCallback) body.getValue();
            assertThat(callback.idDonacion()).isEqualTo(idDonacion);
            assertThat(callback.motivoFallo()).isEqualTo("Destinatario ausente");
            assertThat(callback.replanificable()).isTrue();
        }

        @Test
        @DisplayName("ACEPTADO no le interesa a Donaciones: no se reenvía")
        void aceptadoSeIgnora() {
            assertThat(servicio.procesarEventoExterno(evento("ACEPTADO", null, null))).isFalse();
            verify(donacionesClient, never()).reenviar(anyString(), any());
        }

        @Test
        @DisplayName("Un shipmentRef que no es un id de donación lanza EventoInvalidoException")
        void shipmentRefInvalido() {
            EventoExternoDTO evento = new EventoExternoDTO("EXT-1", "no-es-uuid", "ENTREGADO", "AB123CD", null, null, cuando);

            assertThatThrownBy(() -> servicio.procesarEventoExterno(evento))
                    .isInstanceOf(EventoInvalidoException.class);
            verifyNoInteractions(donacionesClient);
        }
    }
}
