package ar.utn.donatrack.logistica.repositories;

import ar.utn.donatrack.logistica.interfaces.repositories.EntregaRepositoryInterface;
import ar.utn.donatrack.logistica.models.entrega.Entrega;
import ar.utn.donatrack.logistica.models.entrega.EstadoEntrega;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class EntregaRepository implements EntregaRepositoryInterface {

    private final ConcurrentHashMap<UUID, Entrega> storage = new ConcurrentHashMap<>();

    @Override
    public void guardar(Entrega entrega) {
        storage.put(entrega.getId(), entrega);
    }

    @Override
    public Entrega buscarPorId(UUID id) {
        return storage.get(id);
    }

    @Override
    public List<Entrega> buscarPorRutaId(UUID rutaId) {
        return storage.values().stream()
                .filter(e -> rutaId.equals(e.getRutaId()))
                .toList();
    }

    @Override
    public List<Entrega> buscarPorEstado(EstadoEntrega estado) {
        return storage.values().stream()
                .filter(e -> e.getEstado() == estado)
                .toList();
    }
}
