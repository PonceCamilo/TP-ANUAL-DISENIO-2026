package ar.utn.donatrack.logistica.repositories;

import ar.utn.donatrack.logistica.interfaces.repositories.RutaRepositoryInterface;
import ar.utn.donatrack.logistica.models.planificacion.Ruta;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RutaRepository extends RepositorioJpa<Ruta> implements RutaRepositoryInterface {

    public RutaRepository() {
        super(Ruta.class);
    }

    @Override
    public void guardar(Ruta ruta) {
        // Las paradas y entregas se guardan en cascada con la ruta.
        guardarEntidad(ruta, ruta.getId());
    }

    @Override
    public Ruta buscarPorId(UUID id) {
        return buscarEntidad(id);
    }

    @Override
    public List<Ruta> buscarPorCamionId(UUID camionId) {
        return em.createQuery("SELECT r FROM Ruta r WHERE r.camion.id = :camionId", Ruta.class)
                .setParameter("camionId", camionId)
                .getResultList();
    }

    @Override
    public Optional<Ruta> buscarPorEntregaId(UUID entregaId) {
        return em.createQuery(
                        "SELECT r FROM Ruta r JOIN r.paradas p JOIN p.entregas e WHERE e.id = :entregaId",
                        Ruta.class)
                .setParameter("entregaId", entregaId)
                .getResultStream()
                .findFirst();
    }
}
