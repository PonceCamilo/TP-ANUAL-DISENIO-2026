package ar.utn.donatrack.donaciones.repositories;

import ar.utn.donatrack.donaciones.interfaces.repositories.EntidadesBeneficiariasRepositoryInterface;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class EntidadesBeneficiariasRepository implements EntidadesBeneficiariasRepositoryInterface {

    private final Map<UUID, EntidadBeneficiaria> almacenamiento = new ConcurrentHashMap<>();

    @Override
    public void guardar(EntidadBeneficiaria entidad) {
        if (entidad.getId() == null) {
            entidad.setId(UUID.randomUUID());
        }
        almacenamiento.put(entidad.getId(), entidad);
    }

    @Override
    public List<EntidadBeneficiaria> buscarTodas() {
        return almacenamiento.values().stream().toList();
    }

    @Override
    public EntidadBeneficiaria obtenerPorId(UUID id) {
        return almacenamiento.get(id);
    }

    @Override
    public boolean existePorId(UUID id) {
        return almacenamiento.containsKey(id);
    }

    @Override
    public void eliminar(UUID id) {
        almacenamiento.remove(id);
    }
}