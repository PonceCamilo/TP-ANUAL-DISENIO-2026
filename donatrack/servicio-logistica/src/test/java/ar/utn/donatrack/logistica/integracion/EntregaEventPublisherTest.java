package ar.utn.donatrack.logistica.integracion;

import ar.utn.donatrack.logistica.eventos.EntregaEvento;
import ar.utn.donatrack.logistica.eventos.TipoEventoLogistica;
import ar.utn.donatrack.logistica.interfaces.integracion.EntregaEventListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;

/** Observer: el publisher debe reenviar el evento a todos los listeners registrados. */
@ExtendWith(MockitoExtension.class)
class EntregaEventPublisherTest {

    @Mock
    private EntregaEventListener listenerN8n;

    @Mock
    private EntregaEventListener otroListener;

    @Test
    @DisplayName("publicar() notifica a todos los listeners suscriptos con el mismo evento")
    void publicarNotificaATodosLosListeners() {
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
    @DisplayName("publicar() sin listeners registrados no falla")
    void publicarSinListenersNoFalla() {
        EntregaEventPublisher publisher = new EntregaEventPublisher(List.of());

        EntregaEvento evento = EntregaEvento.builder().tipo(TipoEventoLogistica.ENTREGA_CONFIRMADA).build();

        publisher.publicar(evento);
    }
}
