package ar.utn.donatrack.donaciones.clientes;

import ar.utn.donatrack.donaciones.integracion.notificaciones.SolicitudNotificacionMensaje;
import ar.utn.donatrack.donaciones.interfaces.integracion.NotificacionTransport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests de la fachada de notificaciones.
 *
 * NotificacionClient es el único punto por donde el dominio pide que se envíe un
 * aviso. Su trabajo es armar el mensaje y delegarlo al transporte configurado,
 * sin saber si es HTTP o una cola.
 *
 * Esta indirección es la que permitió cumplir el requisito de integración de la
 * Entrega 4 sin tocar ni una línea de DonacionService, LogisticaEventosService o
 * InactividadDonantesService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificacionClient - fachada hacia el Servicio de Notificaciones")
class NotificacionClientTest {

    @Mock
    private NotificacionTransport transporte;

    @InjectMocks
    private NotificacionClient client;

    @Captor
    private ArgumentCaptor<SolicitudNotificacionMensaje> mensajeCaptor;

    @Test
    @DisplayName("Arma el mensaje con los tres campos y lo delega al transporte")
    void delegaEnElTransporte() {
        client.enviarNotificacion("juan@example.com", "Tu donación llegó a destino", "EMAIL");

        verify(transporte).enviar(mensajeCaptor.capture());
        SolicitudNotificacionMensaje enviado = mensajeCaptor.getValue();

        assertThat(enviado.destinatario()).isEqualTo("juan@example.com");
        assertThat(enviado.mensaje()).isEqualTo("Tu donación llegó a destino");
        assertThat(enviado.medio()).isEqualTo("EMAIL");
    }

    @Test
    @DisplayName("No interpreta ni valida el contenido: lo pasa tal cual")
    void noAlteraElContenido() {
        // La validación es responsabilidad del Servicio de Notificaciones, que
        // tiene @NotBlank en su DTO. Duplicarla acá acoplaría los dos servicios.
        client.enviarNotificacion("1155667788", "", "SMS");

        verify(transporte).enviar(mensajeCaptor.capture());
        assertThat(mensajeCaptor.getValue().mensaje()).isEmpty();
    }

    @Test
    @DisplayName("Registra en el arranque qué transporte quedó activo")
    void logueaElTransporteActivo() {
        // Sirve para no tener que adivinar, al mirar los logs de un contenedor,
        // si está usando la cola o las llamadas HTTP.
        when(transporte.nombre()).thenReturn("AMQP/RabbitMQ (asincrónico)");

        client.registrarTransporteActivo();

        verify(transporte).nombre();
    }
}
