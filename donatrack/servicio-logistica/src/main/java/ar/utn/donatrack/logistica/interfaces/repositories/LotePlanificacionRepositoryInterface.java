package ar.utn.donatrack.logistica.interfaces.repositories;

import ar.utn.donatrack.logistica.models.planificacion.LotePlanificacion;

import java.util.UUID;

public interface LotePlanificacionRepositoryInterface {
    void guardar(LotePlanificacion lote);
    LotePlanificacion buscarPorId(UUID id);
}
