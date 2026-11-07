package ar.utn.donatrack.donaciones.clientes;

import ar.utn.donatrack.donaciones.integracion.notificaciones.SolicitudNotificacionMensaje;
import ar.utn.donatrack.donaciones.salida.BandejaDeSalida;
import ar.utn.donatrack.donaciones.salida.Destinos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificacionClient - deja los avisos en la bandeja de salida")
class NotificacionClientTest {

    @Mock
    private BandejaDeSalida salida;

    @InjectMocks
    private NotificacionClient client;

    @Test
    @DisplayName("Arma el mensaje con los tres campos y lo deja en la bandeja (no envía en el momento)")
    void dejaElAvisoEnLaBandeja() {
        client.enviarNotificacion("juan@example.com", "Tu donación llegó a destino", "EMAIL");

        verify(salida).encolar(Destinos.NOTIFICACION,
                new SolicitudNotificacionMensaje("juan@example.com", "Tu donación llegó a destino", "EMAIL"));
    }

    @Test
    @DisplayName("No interpreta ni valida el contenido: lo pasa tal cual")
    void noAlteraElContenido() {
        // La validación es responsabilidad del Servicio de Notificaciones, que
        // tiene @NotBlank en su DTO. Duplicarla acá acoplaría los dos servicios.
        client.enviarNotificacion("1155667788", "", "SMS");

        verify(salida).encolar(Destinos.NOTIFICACION, new SolicitudNotificacionMensaje("1155667788", "", "SMS"));
    }
}
