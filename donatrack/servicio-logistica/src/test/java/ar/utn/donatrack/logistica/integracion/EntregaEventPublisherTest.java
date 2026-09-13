package ar.utn.donatrack.logistica.integracion;

import ar.utn.donatrack.logistica.eventos.EntregaEvento;
import ar.utn.donatrack.logistica.eventos.TipoEventoLogistica;
import ar.utn.donatrack.logistica.interfaces.integracion.EntregaEventListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;

/**
 * Tests del Observer que reenvía hechos de logística a los listeners.
 *
 * El publisher no conoce n8n: recorre la lista de EntregaEventListener y
 * les entrega el mismo evento. El webhook HTTP concreto (N8nLogisticaWebhookListener)
 * no se prueba acá.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EntregaEventPublisher - reenvío de eventos a los listeners")
class EntregaEventPublisherTest {

    @Mock
    private EntregaEventListener listenerN8n;

    @Mock
    private EntregaEventListener otroListener;

    @Nested
    @DisplayName("publicar()")
    class Publicar {

        @Test
        @DisplayName("Notifica a todos los listeners suscriptos con el mismo evento")
        void notificaATodos() {
            EntregaEventPublisher publisher = new EntregaEventPublisher(List.of(listenerN8n, otroListener));
            EntregaEvento evento = EntregaEvento.builder()
                    .tipo(TipoEventoLogistica.INICIO_RUTA)
                    .entregaId(UUID.randomUUID())
                    .build();

            publisher.publicar(evento);

            verify(listenerN8n).onEvento(evento);
            verify(otroListener).onEvento(evento);
        }

        @Test
        @DisplayName("Sin listeners registrados no falla")
        void sinListenersNoFalla() {
            EntregaEventPublisher publisher = new EntregaEventPublisher(List.of());
            EntregaEvento evento = EntregaEvento.builder()
                    .tipo(TipoEventoLogistica.ENTREGA_CONFIRMADA)
                    .build();

            assertThatCode(() -> publisher.publicar(evento)).doesNotThrowAnyException();
        }
    }
}
