package ar.utn.donatrack.logistica.interfaces.repositories;

import ar.utn.donatrack.logistica.models.flota.Camion;

import java.util.List;
import java.util.UUID;

public interface CamionRepositoryInterface {
    void guardar(Camion camion);
    List<Camion> buscarTodos();
    Camion buscarPorId(UUID id);
    List<Camion> buscarPorIds(List<UUID> ids);
}
