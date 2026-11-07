package ar.utn.donatrack.logistica.interfaces.repositories;

import ar.utn.donatrack.logistica.models.entrega.Entrega;
import ar.utn.donatrack.logistica.models.entrega.EstadoEntrega;

import java.util.List;
import java.util.UUID;

/**
 * Solo consultas. La entrega vive embebida en su ruta: para guardar un cambio
 * se guarda la ruta (RutaRepositoryInterface.guardar).
 */
public interface EntregaRepositoryInterface {
    Entrega buscarPorId(UUID id);
    List<Entrega> buscarPorEstado(EstadoEntrega estado);
    List<Entrega> buscarPorIdDonacion(UUID idDonacion);
}
