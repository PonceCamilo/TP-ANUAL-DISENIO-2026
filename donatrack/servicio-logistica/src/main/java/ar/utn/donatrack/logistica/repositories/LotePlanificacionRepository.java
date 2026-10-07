package ar.utn.donatrack.logistica.repositories;

import ar.utn.donatrack.logistica.interfaces.repositories.LotePlanificacionRepositoryInterface;
import ar.utn.donatrack.logistica.models.planificacion.LotePlanificacion;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class LotePlanificacionRepository extends RepositorioJpa<LotePlanificacion>
        implements LotePlanificacionRepositoryInterface {

    public LotePlanificacionRepository() {
        super(LotePlanificacion.class);
    }

    @Override
    public void guardar(LotePlanificacion lote) {
        if (lote.getDonaciones() != null) {
            lote.getDonaciones().forEach(d -> registrarReferencias(d.getIdDonacion(), d.getIdEntidadBeneficiaria()));
        }
        guardarEntidad(lote, lote.getId());
    }

    @Override
    public LotePlanificacion buscarPorId(UUID id) {
        return buscarEntidad(id);
    }
}
