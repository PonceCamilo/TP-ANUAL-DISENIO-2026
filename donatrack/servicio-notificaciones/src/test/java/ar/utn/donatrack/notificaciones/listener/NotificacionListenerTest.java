package ar.utn.donatrack.notificaciones.listener;

import ar.utn.donatrack.notificaciones.dto.SolicitudNotificacionDto;
import ar.utn.donatrack.notificaciones.interfaces.services.NotificacionServiceInterface;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificacionListener - consumidor de cola RabbitMQ")
class NotificacionListenerTest {

    @Mock
    private NotificacionServiceInterface notificacionService;

    private NotificacionListener listener;

    @BeforeEach
    void prepararEscenario() {
        listener = new NotificacionListener(notificacionService);
    }

    @Nested
    @DisplayName("Recepción de mensajes de la cola")
    class RecepcionMensajes {

        @Test
        @DisplayName("Al recibir un mensaje, delega al NotificacionService")
        void delegaAlService() {
            SolicitudNotificacionDto solicitud = new SolicitudNotificacionDto(
                    "ana@mail.com",
                    "Tu donación fue asignada",
                    "EMAIL",
                    null
            );

            listener.recibir(solicitud);

            verify(notificacionService).enviar(solicitud);
        }

        @Test
        @DisplayName("Al recibir un mensaje SMS, también delega al service")
        void delegaAlServiceConSms() {
            SolicitudNotificacionDto solicitud = new SolicitudNotificacionDto(
                    "+54 11 1234-5678",
                    "Misión cumplida",
                    "SMS",
                    null
            );

            listener.recibir(solicitud);

            verify(notificacionService).enviar(solicitud);
        }

        @Test
        @DisplayName("El listener no hace nada más además de delegar al service")
        void soloDelega() {
            SolicitudNotificacionDto solicitud = new SolicitudNotificacionDto(
                    "ana@mail.com",
                    "Hola",
                    "EMAIL",
                    null
            );

            listener.recibir(solicitud);

            verify(notificacionService).enviar(solicitud);
            verifyNoMoreInteractions(notificacionService);
        }
    }
}