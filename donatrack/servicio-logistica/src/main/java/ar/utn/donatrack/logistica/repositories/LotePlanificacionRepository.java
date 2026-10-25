package ar.utn.donatrack.logistica.repositories;

import ar.utn.donatrack.logistica.interfaces.repositories.LotePlanificacionRepositoryInterface;
import ar.utn.donatrack.logistica.models.planificacion.LotePlanificacion;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class LotePlanificacionRepository implements LotePlanificacionRepositoryInterface {

    private final MongoTemplate mongo;

    public LotePlanificacionRepository(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public void guardar(LotePlanificacion lote) {
        mongo.save(lote);
    }

    @Override
    public LotePlanificacion buscarPorId(UUID id) {
        return id == null ? null : mongo.findById(id, LotePlanificacion.class);
    }
}
