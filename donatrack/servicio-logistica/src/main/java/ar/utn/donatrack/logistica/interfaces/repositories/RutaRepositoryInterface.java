package ar.utn.donatrack.logistica.interfaces.repositories;

import ar.utn.donatrack.logistica.models.planificacion.Ruta;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RutaRepositoryInterface {
    void guardar(Ruta ruta);
    Ruta buscarPorId(UUID id);
    List<Ruta> buscarPorCamionId(UUID camionId);

    /**
     * Query inversa del agregado: Ruta → Parada → Entrega.
     * Equivale al JOIN de JPA
     * {@code SELECT r FROM Ruta r JOIN r.paradas p JOIN p.entregas e WHERE e.id = :entregaId}.
     */
    Optional<Ruta> buscarPorEntregaId(UUID entregaId);
}
