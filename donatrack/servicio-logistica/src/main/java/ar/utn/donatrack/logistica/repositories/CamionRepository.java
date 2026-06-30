package ar.utn.donatrack.logistica.repositories;

import ar.utn.donatrack.logistica.interfaces.repositories.CamionRepositoryInterface;
import ar.utn.donatrack.logistica.models.flota.Camion;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class CamionRepository implements CamionRepositoryInterface {

    private final ConcurrentHashMap<UUID, Camion> storage = new ConcurrentHashMap<>();

    @Override
    public void guardar(Camion camion) {
        storage.put(camion.getId(), camion);
    }

    @Override
    public List<Camion> buscarTodos() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public Camion buscarPorId(UUID id) {
        return storage.get(id);
    }

    @Override
    public List<Camion> buscarPorIds(List<UUID> ids) {
        return ids.stream().map(storage::get).filter(java.util.Objects::nonNull).toList();
    }
}
