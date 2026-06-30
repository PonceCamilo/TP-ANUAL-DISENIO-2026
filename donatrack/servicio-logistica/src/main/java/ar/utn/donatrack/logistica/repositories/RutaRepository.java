package ar.utn.donatrack.logistica.repositories;

import ar.utn.donatrack.logistica.interfaces.repositories.RutaRepositoryInterface;
import ar.utn.donatrack.logistica.models.planificacion.Ruta;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class RutaRepository implements RutaRepositoryInterface {

    private final ConcurrentHashMap<UUID, Ruta> storage = new ConcurrentHashMap<>();

    @Override
    public void guardar(Ruta ruta) {
        storage.put(ruta.getId(), ruta);
    }

    @Override
    public Ruta buscarPorId(UUID id) {
        return storage.get(id);
    }

    @Override
    public List<Ruta> buscarPorCamionId(UUID camionId) {
        return storage.values().stream()
                .filter(r -> camionId.equals(r.getCamionId()))
                .toList();
    }
}
