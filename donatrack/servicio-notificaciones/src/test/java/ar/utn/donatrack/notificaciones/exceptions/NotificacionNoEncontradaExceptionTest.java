package ar.utn.donatrack.notificaciones.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NotificacionNoEncontradaException - excepción de dominio")
class NotificacionNoEncontradaExceptionTest {

    @Test
    @DisplayName("El mensaje incluye el id de la notificación no encontrada")
    void mensajeIncluyeId() {
        UUID id = UUID.randomUUID();
        NotificacionNoEncontradaException ex = new NotificacionNoEncontradaException(id);
        assertThat(ex.getMessage()).contains(id.toString());
    }

    @Test
    @DisplayName("Es una RuntimeException")
    void esRuntimeException() {
        UUID id = UUID.randomUUID();
        NotificacionNoEncontradaException ex = new NotificacionNoEncontradaException(id);
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }
}