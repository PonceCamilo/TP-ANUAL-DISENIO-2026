package ar.utn.donatrack.logistica.eventos;

/**
 * Hechos de logística que disparan una notificación. Es deliberadamente
 * un enum propio (no se reutiliza TipoEvento de servicio-notificaciones):
 * logística publica estos eventos hacia el broker de logística, que los
 * reenvía a Donaciones, y es Donaciones quien decide cómo notificar.
 */
public enum TipoEventoLogistica {
    INICIO_RUTA,
    ENTREGA_CONFIRMADA,
    ENTREGA_NO_RECIBIDA
}
