package ar.utn.donatrack.notificaciones.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SolicitudNotificacionDto - contrato de entrada")
class SolicitudNotificacionDtoTest {

    @Nested
    @DisplayName("Construcción del DTO")
    class Construccion {

        @Test
        @DisplayName("Guarda correctamente el destinatario")
        void guardaDestinatario() {
            SolicitudNotificacionDto dto = new SolicitudNotificacionDto(
                    "ana@mail.com", "Hola", "EMAIL", null);
            assertThat(dto.destinatario()).isEqualTo("ana@mail.com");
        }

        @Test
        @DisplayName("Guarda correctamente el mensaje")
        void guardaMensaje() {
            SolicitudNotificacionDto dto = new SolicitudNotificacionDto(
                    "ana@mail.com", "Tu donación fue asignada", "EMAIL", null);
            assertThat(dto.mensaje()).isEqualTo("Tu donación fue asignada");
        }

        @Test
        @DisplayName("Guarda correctamente el medio")
        void guardaMedio() {
            SolicitudNotificacionDto dto = new SolicitudNotificacionDto(
                    "ana@mail.com", "Hola", "SMS", null);
            assertThat(dto.medio()).isEqualTo("SMS");
        }

        @Test
        @DisplayName("El evento puede ser null")
        void eventoNullable() {
            SolicitudNotificacionDto dto = new SolicitudNotificacionDto(
                    "ana@mail.com", "Hola", "EMAIL", null);
            assertThat(dto.evento()).isNull();
        }

        @Test
        @DisplayName("El evento puede tener un valor")
        void eventoConValor() {
            SolicitudNotificacionDto dto = new SolicitudNotificacionDto(
                    "ana@mail.com", "Hola", "EMAIL", "MISION_CUMPLIDA");
            assertThat(dto.evento()).isEqualTo("MISION_CUMPLIDA");
        }
    }
}