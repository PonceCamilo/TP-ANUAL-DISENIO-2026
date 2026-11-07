package ar.utn.donatrack.donaciones.models.donacion.bien;

/**
 * Los tres tipos de bien. Los nombres coinciden con el discriminador que se
 * guarda en la columna `tipo_bien` y con el campo "tipo" del JSON.
 */
public enum TipoBien {
    /** Tiene fecha de vencimiento (alimentos, medicamentos). */
    PERECIBLE,
    /** Importa si es nuevo o usado (mobiliario, vestimenta). */
    CON_ESTADO,
    /** Ni vence ni interesa su estado. */
    GENERICO
}
