package ar.utn.donatrack.donaciones.models.entidad.necesidad.periodicidades;

/**
 * Períodos posibles de una NecesidadRecurrente.
 * Se modela como enum (conjunto fijo y conocido) para que Jackson pueda
 * deserializarlo directamente desde el JSON (ej: "SEMANAL") y para evitar
 * la jerarquía de subclases vacías que no se podía instanciar vía REST.
 * Cada valor conoce su duración en días, usada para calcular el vencimiento.
 */
public enum Periodicidad {
    SEMANAL(7),
    MENSUAL(30),
    CUATRIMESTRAL(120),
    ANUAL(365);

    private final int dias;

    Periodicidad(int dias) {
        this.dias = dias;
    }

    public int getDias() {
        return dias;
    }
}
