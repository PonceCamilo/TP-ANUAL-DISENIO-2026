package ar.utn.donatrack.logistica.eventos;

/**
 * Hechos de logística que disparan una notificación. Es deliberadamente
 * un enum propio (no se reutiliza TipoEvento de servicio-notificaciones):
 * logística publica estos eventos hacia n8n y es n8n quien decide cómo
 * traducirlos a una notificación real.
 */
public enum TipoEventoLogistica {
    INICIO_RUTA,
    ENTREGA_CONFIRMADA,
    ENTREGA_NO_RECIBIDA
}
