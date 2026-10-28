package ar.utn.donatrack.donaciones.models.entidad.necesidad.periodicidades;

import java.time.LocalDate;
import java.time.Period;

/**
 * Períodos posibles de una NecesidadRecurrente.
 * Se modela como enum (conjunto fijo y conocido) para que Jackson pueda
 * deserializarlo directamente desde el JSON (ej: "SEMANAL") y para evitar
 * la jerarquía de subclases vacías que no se podía instanciar vía REST.
 *
 * Cada valor conoce su duración como java.time.Period: un mes es un mes de
 * calendario y no 30 días fijos, así el período mensual que arranca el 1 de
 * marzo termina el 1 de abril y no el 31 de marzo.
 */
public enum Periodicidad {
    SEMANAL(Period.ofWeeks(1)),
    MENSUAL(Period.ofMonths(1)),
    CUATRIMESTRAL(Period.ofMonths(4)),
    ANUAL(Period.ofYears(1));

    private final Period duracion;

    Periodicidad(Period duracion) {
        this.duracion = duracion;
    }

    /** Primer día del período siguiente al que empieza en esa fecha (el fin es exclusivo). */
    public LocalDate finDelPeriodo(LocalDate inicio) {
        return inicio.plus(duracion);
    }

    /**
     * Inicio del período que contiene a la fecha, contando períodos enteros desde
     * el inicio dado. Multiplica la duración en lugar de sumarla de a una: así un
     * período mensual que arrancó un 31 vuelve al 31 cuando el mes lo tiene.
     */
    public LocalDate inicioDelPeriodoVigente(LocalDate inicio, LocalDate fecha) {
        int periodos = 0;
        while (!fecha.isBefore(inicio.plus(duracion.multipliedBy(periodos + 1)))) {
            periodos++;
        }
        return inicio.plus(duracion.multipliedBy(periodos));
    }
}
