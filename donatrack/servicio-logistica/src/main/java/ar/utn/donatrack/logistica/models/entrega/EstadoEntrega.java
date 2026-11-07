package ar.utn.donatrack.logistica.models.entrega;

/**
 * LISTO_PARA_ENTREGAR: ruta planificada, esperando inicio de ruta, o devuelta
 * al depósito tras una entrega no recibida.
 * EN_TRASLADO: el chofer inició la ruta y va camino a la entidad.
 * ENTREGADA: la entidad confirmó la recepción.
 * NO_RECIBIDA: la entidad informó que no recibió la entrega; queda a revisión de administradores.
 */
public enum EstadoEntrega {
    LISTO_PARA_ENTREGAR,
    EN_TRASLADO,
    ENTREGADA,
    NO_RECIBIDA
}
