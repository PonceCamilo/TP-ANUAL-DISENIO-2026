package ar.utn.donatrack.logistica.repositories;

import ar.utn.donatrack.logistica.interfaces.repositories.CamionRepositoryInterface;
import ar.utn.donatrack.logistica.models.flota.Camion;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
public class CamionRepository implements CamionRepositoryInterface {

    private final MongoTemplate mongo;

    public CamionRepository(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public void guardar(Camion camion) {
        mongo.save(camion);
    }

    @Override
    public List<Camion> buscarTodos() {
        Query query = new Query().with(Sort.by(Sort.Direction.ASC, "patente"));
        return mongo.find(query, Camion.class);
    }

    @Override
    public Camion buscarPorId(UUID id) {
        return id == null ? null : mongo.findById(id, Camion.class);
    }

    @Override
    public Camion buscarPorPatente(String patente) {
        return mongo.findOne(Query.query(Criteria.where("patente").is(patente)), Camion.class);
    }

    /**
     * Devuelve los camiones encontrados respetando el orden de los ids pedidos,
     * sin repetir.
     */
    @Override
    public List<Camion> buscarPorIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<UUID> idsValidos = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (idsValidos.isEmpty()) {
            return List.of();
        }
        Map<UUID, Camion> porId = mongo.find(Query.query(Criteria.where("_id").in(idsValidos)), Camion.class)
                .stream()
                .collect(Collectors.toMap(Camion::getId, Function.identity()));
        return idsValidos.stream()
                .map(porId::get)
                .filter(Objects::nonNull)
                .toList();
    }
}
