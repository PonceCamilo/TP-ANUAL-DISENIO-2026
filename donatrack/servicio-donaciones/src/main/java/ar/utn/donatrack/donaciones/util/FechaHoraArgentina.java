package ar.utn.donatrack.donaciones.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Fuente única de verdad de la fecha y hora del sistema, fijada a la zona horaria
 * de Buenos Aires, Argentina. Centraliza las llamadas a now() para no depender de
 * la zona horaria por defecto de la JVM (que cambia según el entorno de despliegue).
 */
public final class FechaHoraArgentina {

    public static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");

    private FechaHoraArgentina() {
    }

    public static LocalDateTime ahora() {
        return LocalDateTime.now(ZONA);
    }

    public static LocalDate hoy() {
        return LocalDate.now(ZONA);
    }
}
