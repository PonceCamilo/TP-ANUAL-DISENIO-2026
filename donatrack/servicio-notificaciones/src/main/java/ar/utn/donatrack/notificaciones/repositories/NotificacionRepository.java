package ar.utn.donatrack.notificaciones.repositories;

import ar.utn.donatrack.notificaciones.interfaces.repositories.NotificacionRepositoryInterface;
import ar.utn.donatrack.notificaciones.model.Notificacion;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class NotificacionRepository implements NotificacionRepositoryInterface {

    private final ConcurrentHashMap<UUID, Notificacion> storage = new ConcurrentHashMap<>();

    @Override
    public void guardar(Notificacion notificacion) {
        storage.put(notificacion.getId(), notificacion);
    }

    @Override
    public List<Notificacion> buscarTodas() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public Notificacion buscarPorId(UUID id) {
        return storage.get(id);
    }
}
