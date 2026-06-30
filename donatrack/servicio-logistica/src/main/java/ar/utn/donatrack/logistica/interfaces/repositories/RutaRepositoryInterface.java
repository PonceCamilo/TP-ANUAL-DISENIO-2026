package ar.utn.donatrack.logistica.interfaces.repositories;

import ar.utn.donatrack.logistica.models.planificacion.Ruta;

import java.util.List;
import java.util.UUID;

public interface RutaRepositoryInterface {
    void guardar(Ruta ruta);
    Ruta buscarPorId(UUID id);
    List<Ruta> buscarPorCamionId(UUID camionId);
}
