package ar.utn.donatrack.logistica.repositories;

import ar.utn.donatrack.logistica.interfaces.repositories.LotePlanificacionRepositoryInterface;
import ar.utn.donatrack.logistica.models.planificacion.LotePlanificacion;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class LotePlanificacionRepository implements LotePlanificacionRepositoryInterface {

    private final ConcurrentHashMap<UUID, LotePlanificacion> storage = new ConcurrentHashMap<>();

    @Override
    public void guardar(LotePlanificacion lote) {
        storage.put(lote.getId(), lote);
    }

    @Override
    public LotePlanificacion buscarPorId(UUID id) {
        return storage.get(id);
    }
}
