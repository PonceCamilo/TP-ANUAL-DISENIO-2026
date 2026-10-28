package ar.utn.donatrack.donaciones.salida;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MensajeSaliente - estados y reintentos de un aviso")
class MensajeSalienteTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 27, 12, 0);

    @Test
    @DisplayName("Nace pendiente y listo para enviarse")
    void naceListo() {
        MensajeSaliente mensaje = MensajeSaliente.nuevo(Destinos.NOTIFICACION, "{}", AHORA);

        assertThat(mensaje.getEstado()).isEqualTo(EstadoMensaje.PENDIENTE);
        assertThat(mensaje.getProximoIntento()).isEqualTo(AHORA);
        assertThat(mensaje.getIntentos()).isZero();
        assertThat(mensaje.isNew()).isTrue();
    }

    @Test
    @DisplayName("Cada falla duplica la espera hasta el próximo intento (30 s, 1 min, 2 min…)")
    void esperaExponencial() {
        MensajeSaliente mensaje = MensajeSaliente.nuevo(Destinos.NOTIFICACION, "{}", AHORA);

        mensaje.registrarFalla("caído", AHORA, 10);
        assertThat(mensaje.getProximoIntento()).isEqualTo(AHORA.plusSeconds(30));
        mensaje.registrarFalla("caído", AHORA, 10);
        assertThat(mensaje.getProximoIntento()).isEqualTo(AHORA.plusSeconds(60));
        mensaje.registrarFalla("caído", AHORA, 10);
        assertThat(mensaje.getProximoIntento()).isEqualTo(AHORA.plusSeconds(120));

        assertThat(mensaje.getEstado()).isEqualTo(EstadoMensaje.PENDIENTE);
        assertThat(mensaje.getIntentos()).isEqualTo(3);
        assertThat(mensaje.getUltimoError()).isEqualTo("caído");
    }

    @Test
    @DisplayName("La espera no pasa de una hora")
    void esperaMaxima() {
        MensajeSaliente mensaje = MensajeSaliente.nuevo(Destinos.NOTIFICACION, "{}", AHORA);
        for (int i = 0; i < 9; i++) {
            mensaje.registrarFalla("caído", AHORA, 50);
        }

        assertThat(mensaje.getProximoIntento()).isEqualTo(AHORA.plusHours(1));
    }

    @Test
    @DisplayName("Al agotar los intentos queda FALLIDO y deja de reintentarse")
    void agotaLosIntentos() {
        MensajeSaliente mensaje = MensajeSaliente.nuevo(Destinos.NOTIFICACION, "{}", AHORA);

        mensaje.registrarFalla("uno", AHORA, 2);
        mensaje.registrarFalla("dos", AHORA, 2);

        assertThat(mensaje.getEstado()).isEqualTo(EstadoMensaje.FALLIDO);
        assertThat(mensaje.getUltimoError()).isEqualTo("dos");
    }

    @Test
    @DisplayName("Enviado queda con la fecha de envío y sin error")
    void enviado() {
        MensajeSaliente mensaje = MensajeSaliente.nuevo(Destinos.NOTIFICACION, "{}", AHORA);
        mensaje.registrarFalla("caído", AHORA, 10);

        mensaje.marcarEnviado(AHORA.plusMinutes(1));

        assertThat(mensaje.getEstado()).isEqualTo(EstadoMensaje.ENVIADO);
        assertThat(mensaje.getEnviado()).isEqualTo(AHORA.plusMinutes(1));
        assertThat(mensaje.getUltimoError()).isNull();
        assertThat(mensaje.getIntentos()).isEqualTo(2);
    }

    @Test
    @DisplayName("Un error muy largo se recorta al tamaño de la columna")
    void errorLargo() {
        MensajeSaliente mensaje = MensajeSaliente.nuevo(Destinos.NOTIFICACION, "{}", AHORA);

        mensaje.registrarFalla("x".repeat(5000), AHORA, 10);

        assertThat(mensaje.getUltimoError()).hasSize(1000);
    }
}
