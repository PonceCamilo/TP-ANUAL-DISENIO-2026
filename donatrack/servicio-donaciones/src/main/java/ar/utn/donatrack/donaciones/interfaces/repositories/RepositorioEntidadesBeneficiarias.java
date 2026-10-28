package ar.utn.donatrack.donaciones.interfaces.repositories;

import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioEntidadesBeneficiarias {
    void guardar(EntidadBeneficiaria entidad);
    List<EntidadBeneficiaria> buscarTodas();
    Optional<EntidadBeneficiaria> buscarPorId(UUID id);

    /** Varias entidades en una sola consulta. Las que no existen simplemente no aparecen. */
    List<EntidadBeneficiaria> obtenerPorIds(Collection<UUID> ids);

    void eliminar(EntidadBeneficiaria entidad);
}