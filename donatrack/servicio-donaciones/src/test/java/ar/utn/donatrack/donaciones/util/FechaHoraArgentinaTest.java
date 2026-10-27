package ar.utn.donatrack.donaciones.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FechaHoraArgentina: hora del sistema en Argentina, con reloj reemplazable")
class FechaHoraArgentinaTest {

    @AfterEach
    void restablecer() {
        FechaHoraArgentina.restablecerReloj();
    }

    @Test
    @DisplayName("con un reloj fijo, ahora() y hoy() devuelven ese instante en hora de Argentina")
    void relojFijo() {
        // 03:00 UTC del 1/3/2026 son las 00:00 del mismo día en Argentina (UTC-3).
        FechaHoraArgentina.usarReloj(Clock.fixed(Instant.parse("2026-03-01T03:00:00Z"), ZoneOffset.UTC));

        assertThat(FechaHoraArgentina.ahora()).isEqualTo(LocalDateTime.of(2026, 3, 1, 0, 0));
        assertThat(FechaHoraArgentina.hoy()).isEqualTo(LocalDate.of(2026, 3, 1));
    }

    @Test
    @DisplayName("la fecha depende de la zona de Argentina y no de la del servidor")
    void fechaEnZonaArgentina() {
        // 02:00 UTC del 1/3 todavía es 28/2 en Argentina.
        FechaHoraArgentina.usarReloj(Clock.fixed(Instant.parse("2026-03-01T02:00:00Z"), ZoneOffset.UTC));

        assertThat(FechaHoraArgentina.hoy()).isEqualTo(LocalDate.of(2026, 2, 28));
    }

    @Test
    void restablecerVuelveAlRelojReal() {
        FechaHoraArgentina.usarReloj(Clock.fixed(Instant.parse("2000-01-01T12:00:00Z"), ZoneOffset.UTC));
        FechaHoraArgentina.restablecerReloj();

        assertThat(FechaHoraArgentina.hoy()).isAfter(LocalDate.of(2025, 1, 1));
        assertThat(FechaHoraArgentina.reloj().getZone()).isEqualTo(FechaHoraArgentina.ZONA);
    }
}
