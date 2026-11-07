package ar.utn.donatrack.incentivos.integracion.notificaciones;

/**
 * Puerto de salida hacia el Servicio de Notificaciones.
 *
 * Hay dos implementaciones y se elige por configuración
 * (`notificaciones.transporte`):
 * - NotificacionTransportRest: POST /notificaciones (comportamiento histórico).
 * - NotificacionTransportAmqp: publica en la cola de RabbitMQ.
 */
public interface NotificacionTransport {
    void enviar(SolicitudNotificacionMensaje solicitud);
    String nombre();
}