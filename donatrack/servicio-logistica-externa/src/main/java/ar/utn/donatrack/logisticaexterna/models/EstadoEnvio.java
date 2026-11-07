package ar.utn.donatrack.logisticaexterna.models;

/**
 * Ciclo de vida de un envío:
 *
 * ACEPTADO  -> EN_CAMINO            (despacho)
 * EN_CAMINO -> ENTREGADO | FALLIDO
 * FALLIDO   -> EN_CAMINO            (reintento)
 * ENTREGADO es terminal.
 */
public enum EstadoEnvio {
    ACEPTADO,
    EN_CAMINO,
    ENTREGADO,
    FALLIDO;

    public boolean puedePasarA(EstadoEnvio nuevo) {
        return switch (this) {
            case ACEPTADO -> nuevo == EN_CAMINO;
            case EN_CAMINO -> nuevo == ENTREGADO || nuevo == FALLIDO;
            case FALLIDO -> nuevo == EN_CAMINO;
            case ENTREGADO -> false;
        };
    }
}
