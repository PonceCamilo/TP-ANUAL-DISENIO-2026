package ar.utn.donatrack.notificaciones.interfaces.services;

import ar.utn.donatrack.notificaciones.dto.SolicitudNotificacionDto;
import ar.utn.donatrack.notificaciones.model.Notificacion;

import java.util.List;
import java.util.UUID;

public interface NotificacionServiceInterface {
    void enviar(SolicitudNotificacionDto solicitud);
    List<Notificacion> obtenerTodas();
    Notificacion obtenerPorId(UUID id);
}
