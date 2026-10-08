package ar.utn.donatrack.incentivos.integracion.notificaciones;

/**
 * Puerto de salida hacia el Servicio de Notificaciones.
 *
 * Permite cambiar el transporte —de HTTP sincrónico a cola asíncrona—
 * sin que IncentivosService se entere: sigue usando NotificacionClient,
 * que delega en este puerto.
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