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
 * Solo lee. La entrega no tiene colección propia: vive embebida en la ruta,
 * adentro de sus paradas. Buscar una entrega es buscar esa ruta.
 * Un cambio de estado, fotos o historial no se guarda acá: se guarda la ruta
 * completa con {@code RutaRepository.guardar}.
 */
@Repository
public class EntregaRepository implements EntregaRepositoryInterface {

    private final MongoTemplate mongo;

    public EntregaRepository(MongoTemplate mongo) {
        this.mongo = mongo;
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
