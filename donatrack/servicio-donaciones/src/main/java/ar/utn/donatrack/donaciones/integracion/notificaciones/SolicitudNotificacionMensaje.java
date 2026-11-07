package ar.utn.donatrack.donaciones.integracion.notificaciones;

/**
 * Payload que viaja hacia el Servicio de Notificaciones, por HTTP o por la cola.
 *
 * Replica exactamente los tres campos de SolicitudNotificacionDto de ese
 * servicio (destinatario, mensaje, medio) para que el consumidor pueda
 * deserializarlo sin traducción intermedia.
 *
 * `medio` viaja como String ("EMAIL" / "SMS" / "WHATSAPP") a propósito: cada
 * servicio mantiene su propio modelo (Bounded Context independiente) y no
 * queremos acoplar donaciones a la jerarquía MedioNotificacion de notificaciones.
 */
public record SolicitudNotificacionMensaje(
        String destinatario,
        String mensaje,
        String medio) {
}
