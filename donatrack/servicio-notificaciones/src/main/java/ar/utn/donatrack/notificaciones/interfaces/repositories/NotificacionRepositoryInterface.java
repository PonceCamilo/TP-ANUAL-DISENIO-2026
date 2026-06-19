package ar.utn.donatrack.notificaciones.interfaces.repositories;

import ar.utn.donatrack.notificaciones.model.Notificacion;

import java.util.List;
import java.util.UUID;

public interface NotificacionRepositoryInterface {
    void guardar(Notificacion notificacion);
    List<Notificacion> buscarTodas();
    Notificacion buscarPorId(UUID id);
}