package ar.utn.donatrack.logistica.repositories;

import ar.utn.donatrack.logistica.interfaces.repositories.EntregaRepositoryInterface;
import ar.utn.donatrack.logistica.models.entrega.Entrega;
import ar.utn.donatrack.logistica.models.entrega.EstadoEntrega;
import ar.utn.donatrack.logistica.models.planificacion.Ruta;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * La entrega no tiene colección: vive embebida en la ruta.
 * Guardar una entrega reescribe el documento de su ruta.
 */
@Repository
public class EntregaRepository implements EntregaRepositoryInterface {

    private final MongoTemplate mongo;

    public EntregaRepository(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public void guardar(Entrega entrega) {
        Ruta ruta = mongo.findOne(
                Query.query(Criteria.where("paradas.entregas.id").is(entrega.getId())), Ruta.class);
        if (ruta == null || ruta.getParadas() == null) {
            throw new IllegalStateException("No hay una ruta que contenga la entrega " + entrega.getId());
        }
        boolean reemplazada = false;
        for (var parada : ruta.getParadas()) {
            if (parada.getEntregas() != null
                    && parada.getEntregas().stream().anyMatch(e -> entrega.getId().equals(e.getId()))) {
                parada.reemplazarEntrega(entrega);
                reemplazada = true;
                break;
            }
        }
        if (!reemplazada) {
            throw new IllegalStateException("No hay una ruta que contenga la entrega " + entrega.getId());
        }
        mongo.save(ruta);
    }

    @Override
    public Entrega buscarPorId(UUID id) {
        if (id == null) {
            return null;
        }
        return entregasDe(Query.query(Criteria.where("paradas.entregas.id").is(id))).stream()
                .filter(entrega -> id.equals(entrega.getId()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<Entrega> buscarPorEstado(EstadoEntrega estado) {
        return entregasDe(Query.query(Criteria.where("paradas.entregas.estado").is(estado))).stream()
                .filter(entrega -> entrega.getEstado() == estado)
                .toList();
    }

    @Override
    public List<Entrega> buscarPorIdDonacion(UUID idDonacion) {
        return entregasDe(Query.query(Criteria.where("paradas.entregas.idDonacion").is(idDonacion))).stream()
                .filter(entrega -> idDonacion.equals(entrega.getIdDonacion()))
                .toList();
    }

    private List<Entrega> entregasDe(Query query) {
        List<Entrega> entregas = new ArrayList<>();
        for (Ruta ruta : mongo.find(query, Ruta.class)) {
            ruta.enlazarEntregas();
            entregas.addAll(ruta.obtenerEntregas());
        }
        return entregas;
    }
}
