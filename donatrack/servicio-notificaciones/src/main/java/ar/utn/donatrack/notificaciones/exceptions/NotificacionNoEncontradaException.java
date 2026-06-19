package ar.utn.donatrack.notificaciones.exceptions;

import java.util.UUID;

/**
 * Se lanza cuando se solicita una notificación por id y no existe.
 * El GlobalExceptionHandler la traduce a HTTP 404.
 */
public class NotificacionNoEncontradaException extends RuntimeException {

    public NotificacionNoEncontradaException(UUID id) {
        super("No existe una notificación con id " + id);
    }
}
