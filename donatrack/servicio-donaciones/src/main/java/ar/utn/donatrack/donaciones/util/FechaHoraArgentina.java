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

    /**
     * Identificador de la zona. Es una constante de texto para poder usarla en
     * anotaciones: los @Scheduled la declaran en `zone` y así corren en hora de
     * Argentina aunque el servidor (por ejemplo, un contenedor) esté en UTC.
     */
    public static final String ZONA_ID = "America/Argentina/Buenos_Aires";

    public static final ZoneId ZONA = ZoneId.of(ZONA_ID);

    private FechaHoraArgentina() {
    }

    public static LocalDateTime ahora() {
        return LocalDateTime.now(ZONA);
    }

    public static LocalDate hoy() {
        return LocalDate.now(ZONA);
    }
}
