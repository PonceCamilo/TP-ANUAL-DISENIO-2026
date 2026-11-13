package ar.utn.donatrack.incentivos.client;

import ar.utn.donatrack.incentivos.models.insignias.InsigniaObtenida;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Entrega 6 (I4): si n8n no está levantado, otorgar una insignia sigue funcionando. */
@DisplayName("N8nWebhookClient con n8n caído")
class N8nWebhookClientTest {

    @Test
    @DisplayName("Un webhook que no responde se registra en el log y no corta el flujo de incentivos")
    void n8nCaidoNoFrenaNada() throws Exception {
        int puertoLibre;
        try (ServerSocket socket = new ServerSocket(0)) {
            puertoLibre = socket.getLocalPort();
        }
        N8nWebhookClient cliente = new N8nWebhookClient(
                "http://127.0.0.1:" + puertoLibre + "/webhook/insignia-obtenida", new ObjectMapper());
        InsigniaObtenida insignia = mock(InsigniaObtenida.class, RETURNS_DEEP_STUBS);
        when(insignia.getInsignia().getId()).thenReturn(1L);
        when(insignia.getInsignia().getNombre()).thenReturn("Semilla de Solidaridad");
        when(insignia.getInsignia().getImagen()).thenReturn("semilla.png");
        when(insignia.getFechaObtencion()).thenReturn(java.time.LocalDate.now());

        assertThatCode(() -> cliente.notificarInsigniaObtenida(UUID.randomUUID(), insignia, "ana@example.test"))
                .doesNotThrowAnyException();
    }
}
