package ar.utn.donatrack.incentivos.integracion.notificaciones;

/**
 * Objeto que viaja por la cola de RabbitMQ hacia servicio-notificaciones.
 * Es un record simple — solo transporta los datos necesarios para que
 * servicio-notificaciones pueda armar y enviar la notificación.
 * No comparte clases con servicio-notificaciones a propósito: cada
 * servicio tiene su propio modelo (Bounded Context independiente).
 */
public record SolicitudNotificacionMensaje(
        String destinatario,
        String mensaje,
        String medio,
        String evento
) {}