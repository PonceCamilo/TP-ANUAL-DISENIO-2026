package ar.utn.donatrack.donaciones.util;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Fuente única de verdad de la fecha y hora del sistema, fijada a la zona horaria
 * de Buenos Aires, Argentina. Centraliza las llamadas a now() para no depender de
 * la zona horaria por defecto de la JVM (que cambia según el entorno de despliegue).
 *
 * El reloj es reemplazable: los tests pueden fijar «ahora» en una fecha conocida
 * (usarReloj) y volver al reloj real al terminar (restablecerReloj), en lugar de
 * depender del momento en que corren. Se mantiene estático porque lo usan también
 * las entidades (fecha de una donación, de un cambio de estado), que no reciben
 * dependencias inyectadas.
 */
public final class FechaHoraArgentina {

    /**
     * Identificador de la zona. Es una constante de texto para poder usarla en
     * anotaciones: los @Scheduled la declaran en `zone` y así corren en hora de
     * Argentina aunque el servidor (por ejemplo, un contenedor) esté en UTC.
     */
    public static final String ZONA_ID = "America/Argentina/Buenos_Aires";

    public static final ZoneId ZONA = ZoneId.of(ZONA_ID);

    private static volatile Clock reloj = Clock.system(ZONA);

    private FechaHoraArgentina() {
    }

    public static LocalDateTime ahora() {
        return LocalDateTime.now(reloj);
    }

    public static LocalDate hoy() {
        return LocalDate.now(reloj);
    }

    /** El reloj en uso, para quien necesite un java.time.Clock. */
    public static Clock reloj() {
        return reloj;
    }

    /** Reemplaza el reloj (pensado para tests). Siempre se interpreta en hora de Argentina. */
    public static void usarReloj(Clock nuevoReloj) {
        reloj = nuevoReloj.withZone(ZONA);
    }

    /** Vuelve al reloj real del sistema. */
    public static void restablecerReloj() {
        reloj = Clock.system(ZONA);
    }
}
