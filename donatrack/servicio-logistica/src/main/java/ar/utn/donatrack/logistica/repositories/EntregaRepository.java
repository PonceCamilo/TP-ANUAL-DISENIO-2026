package ar.utn.donatrack.logistica.repositories;

import ar.utn.donatrack.logistica.interfaces.repositories.EntregaRepositoryInterface;
import ar.utn.donatrack.logistica.models.entrega.Entrega;
import ar.utn.donatrack.logistica.models.entrega.EstadoEntrega;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class EntregaRepository extends RepositorioJpa<Entrega> implements EntregaRepositoryInterface {

    public EntregaRepository() {
        super(Entrega.class);
    }

    @Override
    public void guardar(Entrega entrega) {
        registrarReferencias(entrega.getIdDonacion(), entrega.getIdEntidadBeneficiaria());
        guardarEntidad(entrega, entrega.getId());
    }

    @Override
    public Entrega buscarPorId(UUID id) {
        return buscarEntidad(id);
    }

    @Override
    public List<Entrega> buscarPorEstado(EstadoEntrega estado) {
        return em.createQuery("SELECT e FROM Entrega e WHERE e.estado = :estado", Entrega.class)
                .setParameter("estado", estado)
                .getResultList();
    }
}
