package ar.utn.donatrack.notificaciones.interfaces.services;

import ar.utn.donatrack.notificaciones.dto.SolicitudNotificacionDto;

public interface NotificacionServiceInterface {
    void enviar(SolicitudNotificacionDto solicitud);
}
