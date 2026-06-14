package ar.utn.donatrack.notificaciones.service;

import ar.utn.donatrack.notificaciones.model.MedioNotificacion;
import ar.utn.donatrack.notificaciones.model.Notificacion;

import java.util.List;

public interface NotificacionService {

    /** Crea y envía una notificación por el medio indicado. */
    Notificacion enviar(String destinatario, String mensaje, MedioNotificacion medio);

    /** Devuelve todas las notificaciones registradas (para auditoría). */
    List<Notificacion> listarTodas();
}
