package ar.utn.donatrack.logistica.interfaces.repositories;

import ar.utn.donatrack.logistica.models.entrega.Entrega;
import ar.utn.donatrack.logistica.models.entrega.EstadoEntrega;

import java.util.List;
import java.util.UUID;

public interface EntregaRepositoryInterface {
    void guardar(Entrega entrega);
    Entrega buscarPorId(UUID id);
    List<Entrega> buscarPorRutaId(UUID rutaId);
    List<Entrega> buscarPorEstado(EstadoEntrega estado);
}
