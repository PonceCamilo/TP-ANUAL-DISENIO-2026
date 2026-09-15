package ar.utn.donatrack.notificaciones.services;

import ar.utn.donatrack.notificaciones.dto.SolicitudNotificacionDto;
import ar.utn.donatrack.notificaciones.exceptions.NotificacionNoEncontradaException;
import ar.utn.donatrack.notificaciones.factory.NotificadorFactory;
import ar.utn.donatrack.notificaciones.interfaces.repositories.NotificacionRepositoryInterface;
import ar.utn.donatrack.notificaciones.interfaces.services.NotificadorInterface;
import ar.utn.donatrack.notificaciones.model.EstadoNotificacion;
import ar.utn.donatrack.notificaciones.model.Notificacion;
import ar.utn.donatrack.notificaciones.model.medios.Email;
import ar.utn.donatrack.notificaciones.model.medios.Sms;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificacionService - envío y consulta de notificaciones")
class NotificacionServiceTest {

    @Mock
    private NotificacionRepositoryInterface repositorio;

    @Mock
    private NotificadorFactory notificadorFactory;

    @Mock
    private NotificadorInterface notificador;

    private NotificacionService servicio;

    @BeforeEach
    void prepararEscenario() {
        servicio = new NotificacionService(repositorio, notificadorFactory);
    }

    private SolicitudNotificacionDto solicitud(String destinatario, String mensaje, String medio) {
        return new SolicitudNotificacionDto(destinatario, mensaje, medio, null);
    }

    @Nested
    @DisplayName("Envío de notificaciones")
    class Envio {

        @Test
        @DisplayName("Una notificación válida se persiste y se envía por el medio correcto")
        void enviaCorrectamente() {
            when(notificadorFactory.obtenerPara("EMAIL")).thenReturn(notificador);
            when(notificador.getMedio()).thenReturn(new Email());

            servicio.enviar(solicitud("ana@mail.com", "Hola!", "EMAIL"));

            verify(notificador).enviar(any(Notificacion.class));
            verify(repositorio, times(2)).guardar(any(Notificacion.class));
        }

        @Test
        @DisplayName("La notificación se crea en estado PENDIENTE antes de enviarse")
        void creaEnPendiente() {
            when(notificadorFactory.obtenerPara("EMAIL")).thenReturn(notificador);
            when(notificador.getMedio()).thenReturn(new Email());

            ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);

            servicio.enviar(solicitud("ana@mail.com", "Hola!", "EMAIL"));

            verify(repositorio, times(2)).guardar(captor.capture());
            assertThat(captor.getAllValues().get(0).getEstado()).isEqualTo(EstadoNotificacion.PENDIENTE);
        }

        @Test
        @DisplayName("La notificación guarda el destinatario y mensaje correctamente")
        void guardaLosDatosCorrectamente() {
            when(notificadorFactory.obtenerPara("SMS")).thenReturn(notificador);
            when(notificador.getMedio()).thenReturn(new Sms());

            ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);

            servicio.enviar(solicitud("+54 11 1234-5678", "Tu donación fue asignada", "SMS"));

            verify(notificador).enviar(captor.capture());
            assertThat(captor.getValue().getDestinatario()).isEqualTo("+54 11 1234-5678");
            assertThat(captor.getValue().getMensaje()).isEqualTo("Tu donación fue asignada");
        }

        @Test
        @DisplayName("El medio se resuelve a través de la factory, no hardcodeado")
        void resuelveElMedioViaFactory() {
            when(notificadorFactory.obtenerPara("EMAIL")).thenReturn(notificador);
            when(notificador.getMedio()).thenReturn(new Email());

            servicio.enviar(solicitud("ana@mail.com", "Hola!", "EMAIL"));

            verify(notificadorFactory).obtenerPara("EMAIL");
        }

        @Test
        @DisplayName("Un medio desconocido lanza excepción antes de persistir nada")
        void medioDesconocidoLanzaExcepcion() {
            when(notificadorFactory.obtenerPara("TELEGRAM"))
                    .thenThrow(new IllegalArgumentException("Medio no soportado: TELEGRAM"));

            assertThatThrownBy(() -> servicio.enviar(solicitud("ana@mail.com", "Hola!", "TELEGRAM")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("TELEGRAM");

            verifyNoInteractions(repositorio);
        }
    }

    @Nested
    @DisplayName("Consulta de notificaciones")
    class Consulta {

        @Test
        @DisplayName("obtenerTodas() devuelve todas las notificaciones guardadas")
        void obtenerTodas() {
            Notificacion n1 = Notificacion.builder()
                    .id(UUID.randomUUID())
                    .destinatario("ana@mail.com")
                    .mensaje("Hola")
                    .medio(new Email())
                    .estado(EstadoNotificacion.ENVIADA)
                    .build();

            when(repositorio.buscarTodas()).thenReturn(List.of(n1));

            assertThat(servicio.obtenerTodas()).hasSize(1);
        }

        @Test
        @DisplayName("obtenerPorId() devuelve la notificación correcta")
        void obtenerPorId() {
            UUID id = UUID.randomUUID();
            Notificacion notificacion = Notificacion.builder()
                    .id(id)
                    .destinatario("ana@mail.com")
                    .mensaje("Hola")
                    .medio(new Email())
                    .estado(EstadoNotificacion.ENVIADA)
                    .build();

            when(repositorio.buscarPorId(id)).thenReturn(notificacion);

            assertThat(servicio.obtenerPorId(id).getId()).isEqualTo(id);
        }

        @Test
        @DisplayName("obtenerPorId() lanza 404 si la notificación no existe")
        void obtenerPorIdInexistente() {
            UUID idInexistente = UUID.randomUUID();
            when(repositorio.buscarPorId(idInexistente)).thenReturn(null);

            assertThatThrownBy(() -> servicio.obtenerPorId(idInexistente))
                    .isInstanceOf(NotificacionNoEncontradaException.class);
        }
    }
}