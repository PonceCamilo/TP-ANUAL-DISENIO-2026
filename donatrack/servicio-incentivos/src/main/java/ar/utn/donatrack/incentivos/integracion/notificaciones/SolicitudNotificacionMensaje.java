package ar.utn.donatrack.incentivos.integracion.notificaciones;

// Objeto que viaja por la cola de RabbitMQ hacia servicio-notificaciones.
public record SolicitudNotificacionMensaje(
        String destinatario,
        String mensaje,
        String medio,
        String evento
) {}