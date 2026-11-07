package ar.utn.donatrack.notificaciones.notificador;

import ar.utn.donatrack.notificaciones.model.EstadoNotificacion;
import ar.utn.donatrack.notificaciones.model.Notificacion;
import ar.utn.donatrack.notificaciones.model.medios.Email;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NotificadorBase - esqueleto Template Method")
class NotificadorBaseTest {

    private Notificacion notificacion;

    @BeforeEach
    void prepararEscenario() {
        notificacion = Notificacion.builder()
                .id(UUID.randomUUID())
                .destinatario("ana@mail.com")
                .mensaje("Hola!")
                .medio(new Email())
                .estado(EstadoNotificacion.PENDIENTE)
                .build();
    }

    /**
     * Implementación concreta de prueba que simula un envío exitoso.
     */
    private static class NotificadorExitoso extends NotificadorBase {
        @Override
        protected void realizarEnvio(Notificacion notificacion) {
            // simula envío exitoso sin hacer nada
        }

        @Override
        protected String getEtiqueta() {
            return "TEST";
        }

        @Override
        public ar.utn.donatrack.notificaciones.model.medios.MedioNotificacion getMedio() {
            return new Email();
        }
    }

    /**
     * Implementación concreta de prueba que simula un envío fallido.
     */
    private static class NotificadorFallido extends NotificadorBase {
        @Override
        protected void realizarEnvio(Notificacion notificacion) throws Exception {
            throw new RuntimeException("Error simulado de envío");
        }

        @Override
        protected String getEtiqueta() {
            return "TEST_FALLIDO";
        }

        @Override
        public ar.utn.donatrack.notificaciones.model.medios.MedioNotificacion getMedio() {
            return new Email();
        }
    }

    @Nested
    @DisplayName("Envío exitoso")
    class EnvioExitoso {

        @Test
        @DisplayName("Si realizarEnvio() no lanza excepción, la notificación queda ENVIADA")
        void marcaEnviada() {
            new NotificadorExitoso().enviar(notificacion);
            assertThat(notificacion.getEstado()).isEqualTo(EstadoNotificacion.ENVIADA);
        }

        @Test
        @DisplayName("Si realizarEnvio() no lanza excepción, se registra la fecha de envío")
        void registraFechaEnvio() {
            new NotificadorExitoso().enviar(notificacion);
            assertThat(notificacion.getFechaEnvio()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Envío fallido")
    class EnvioFallido {

        @Test
        @DisplayName("Si realizarEnvio() lanza excepción, la notificación queda FALLIDA")
        void marcaFallida() {
            new NotificadorFallido().enviar(notificacion);
            assertThat(notificacion.getEstado()).isEqualTo(EstadoNotificacion.FALLIDA);
        }

        @Test
        @DisplayName("Si realizarEnvio() lanza excepción, no se registra fecha de envío")
        void noRegistraFechaEnvio() {
            new NotificadorFallido().enviar(notificacion);
            assertThat(notificacion.getFechaEnvio()).isNull();
        }
    }
}