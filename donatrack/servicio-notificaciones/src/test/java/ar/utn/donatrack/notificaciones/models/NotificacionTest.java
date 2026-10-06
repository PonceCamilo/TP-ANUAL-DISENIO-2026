package ar.utn.donatrack.notificaciones.models;

import ar.utn.donatrack.notificaciones.model.EstadoNotificacion;
import ar.utn.donatrack.notificaciones.model.Notificacion;
import ar.utn.donatrack.notificaciones.model.medios.Email;
import ar.utn.donatrack.notificaciones.model.medios.Sms;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Notificacion - modelo de dominio")
class NotificacionTest {

    private Notificacion notificacionBase() {
        return Notificacion.builder()
                .id(UUID.randomUUID())
                .destinatario("ana@mail.com")
                .mensaje("Hola!")
                .medio(new Email())
                .estado(EstadoNotificacion.PENDIENTE)
                .build();
    }

    @Nested
    @DisplayName("Estado inicial")
    class EstadoInicial {

        @Test
        @DisplayName("Una notificación nueva se crea con estado PENDIENTE")
        void creaEnPendiente() {
            Notificacion n = notificacionBase();
            assertThat(n.getEstado()).isEqualTo(EstadoNotificacion.PENDIENTE);
        }

        @Test
        @DisplayName("Una notificación guarda correctamente el destinatario")
        void guardaDestinatario() {
            Notificacion n = notificacionBase();
            assertThat(n.getDestinatario()).isEqualTo("ana@mail.com");
        }

        @Test
        @DisplayName("Una notificación guarda correctamente el mensaje")
        void guardaMensaje() {
            Notificacion n = notificacionBase();
            assertThat(n.getMensaje()).isEqualTo("Hola!");
        }

        @Test
        @DisplayName("Una notificación guarda correctamente el medio")
        void guardaMedio() {
            Notificacion n = notificacionBase();
            assertThat(n.getMedio()).isInstanceOf(Email.class);
        }
    }

    @Nested
    @DisplayName("Cambios de estado")
    class CambiosDeEstado {

        @Test
        @DisplayName("marcarEnviada() cambia el estado a ENVIADA")
        void marcarEnviada() {
            Notificacion n = notificacionBase();
            n.marcarEnviada();
            assertThat(n.getEstado()).isEqualTo(EstadoNotificacion.ENVIADA);
        }

        @Test
        @DisplayName("marcarEnviada() registra la fecha de envío")
        void marcarEnviadaRegistraFecha() {
            Notificacion n = notificacionBase();
            n.marcarEnviada();
            assertThat(n.getFechaEnvio()).isNotNull();
        }

        @Test
        @DisplayName("marcarFallida() cambia el estado a FALLIDA")
        void marcarFallida() {
            Notificacion n = notificacionBase();
            n.marcarFallida();
            assertThat(n.getEstado()).isEqualTo(EstadoNotificacion.FALLIDA);
        }

        @Test
        @DisplayName("marcarFallida() no registra fecha de envío")
        void marcarFallidaNoRegistraFecha() {
            Notificacion n = notificacionBase();
            n.marcarFallida();
            assertThat(n.getFechaEnvio()).isNull();
        }
    }

    @Nested
    @DisplayName("Medios de notificación")
    class Medios {

        @Test
        @DisplayName("Una notificación puede tener medio EMAIL")
        void medioEmail() {
            Notificacion n = Notificacion.builder()
                    .id(UUID.randomUUID())
                    .destinatario("ana@mail.com")
                    .mensaje("Hola")
                    .medio(new Email())
                    .estado(EstadoNotificacion.PENDIENTE)
                    .build();
            assertThat(n.getMedio().getNombre()).isEqualTo("EMAIL");
        }

        @Test
        @DisplayName("Una notificación puede tener medio SMS")
        void medioSms() {
            Notificacion n = Notificacion.builder()
                    .id(UUID.randomUUID())
                    .destinatario("+54 11 1234-5678")
                    .mensaje("Hola")
                    .medio(new Sms())
                    .estado(EstadoNotificacion.PENDIENTE)
                    .build();
            assertThat(n.getMedio().getNombre()).isEqualTo("SMS");
        }
    }
}