package ar.utn.donatrack.donaciones.interfaces.integracion;

import ar.utn.donatrack.donaciones.integracion.notificaciones.SolicitudNotificacionMensaje;

/**
 * Puerto de salida hacia el Servicio de Notificaciones.
 *
 * Existe para que la Entrega 4 pueda cambiar el transporte —de una llamada HTTP
 * sincrónica a una cola de mensajes— sin que ningún service del dominio se
 * entere: todos siguen usando NotificacionClient, que delega en este puerto.
 *
 * Hay dos implementaciones y se elige por configuración
 * (`notificaciones.transporte`):
 *   - NotificacionTransportRest: POST /notificaciones (comportamiento histórico).
 *   - NotificacionTransportAmqp: publica en la cola de RabbitMQ.
 */
public interface NotificacionTransport {

    /**
     * Entrega la solicitud al Servicio de Notificaciones. Si no pudo, lanza la
     * excepción: el aviso está en la bandeja de salida y RelayDeMensajes lo reintenta.
     */
    void enviar(SolicitudNotificacionMensaje solicitud);

    /** Nombre del transporte, solo para dejar rastro en los logs de arranque. */
    String nombre();
}
