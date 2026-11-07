package ar.utn.donatrack.logistica.repositories;

import ar.utn.donatrack.logistica.interfaces.repositories.CamionRepositoryInterface;
import ar.utn.donatrack.logistica.interfaces.repositories.RutaRepositoryInterface;
import ar.utn.donatrack.logistica.models.flota.Camion;
import ar.utn.donatrack.logistica.models.planificacion.EstadoRuta;
import ar.utn.donatrack.logistica.models.planificacion.Ruta;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
public class RutaRepository implements RutaRepositoryInterface {

    private final MongoTemplate mongo;
    private final CamionRepositoryInterface camionRepositorio;

    public RutaRepository(MongoTemplate mongo, CamionRepositoryInterface camionRepositorio) {
        this.mongo = mongo;
        this.camionRepositorio = camionRepositorio;
    }

    @Override
    public void guardar(Ruta ruta) {
        if (ruta.getCamion() != null) {
            ruta.setCamionId(ruta.getCamion().getId());
        }
        ruta.enlazarEntregas();
        mongo.save(ruta);
    }

    @Override
    public Ruta buscarPorId(UUID id) {
        return preparar(id == null ? null : mongo.findById(id, Ruta.class));
    }

    @Override
    public List<Ruta> buscarPorCamionId(UUID camionId) {
        List<Ruta> rutas = mongo.find(
                Query.query(Criteria.where("camionId").is(camionId)), Ruta.class);
        return preparar(rutas);
    }

    @Override
    public List<Ruta> buscarPorEstado(EstadoRuta estado) {
        List<Ruta> rutas = mongo.find(Query.query(Criteria.where("estado").is(estado)), Ruta.class);
        return preparar(rutas);
    }

    @Override
    public Optional<Ruta> buscarPorEntregaId(UUID entregaId) {
        return mongo.find(
                        Query.query(Criteria.where("paradas.entregas.id").is(entregaId)), Ruta.class)
                .stream()
                .findFirst()
                .map(this::preparar);
    }

    private Ruta preparar(Ruta ruta) {
        if (ruta == null) {
            return null;
        }
        ruta.enlazarEntregas();
        if (ruta.getCamion() == null && ruta.getCamionId() != null) {
            ruta.setCamion(camionRepositorio.buscarPorId(ruta.getCamionId()));
        }
        return ruta;
    }

    private List<Ruta> preparar(List<Ruta> rutas) {
        rutas.forEach(Ruta::enlazarEntregas);
        List<UUID> ids = rutas.stream()
                .filter(ruta -> ruta.getCamion() == null)
                .map(Ruta::getCamionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return rutas;
        }
        Map<UUID, Camion> porId = camionRepositorio.buscarPorIds(ids).stream()
                .collect(Collectors.toMap(Camion::getId, Function.identity()));
        for (Ruta ruta : rutas) {
            if (ruta.getCamion() == null) {
                ruta.setCamion(porId.get(ruta.getCamionId()));
            }
        }
        return rutas;
    }
}
