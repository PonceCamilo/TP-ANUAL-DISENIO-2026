package ar.utn.donatrack.logistica.interfaces.repositories;

import ar.utn.donatrack.logistica.models.planificacion.EstadoRuta;
import ar.utn.donatrack.logistica.models.planificacion.Ruta;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RutaRepositoryInterface {
    void guardar(Ruta ruta);
    Ruta buscarPorId(UUID id);
    List<Ruta> buscarPorCamionId(UUID camionId);

    /**
     * Busca la ruta cuyo documento embebe la entrega indicada.
     */
    Optional<Ruta> buscarPorEntregaId(UUID entregaId);

    List<Ruta> buscarPorEstado(EstadoRuta estado);
}
