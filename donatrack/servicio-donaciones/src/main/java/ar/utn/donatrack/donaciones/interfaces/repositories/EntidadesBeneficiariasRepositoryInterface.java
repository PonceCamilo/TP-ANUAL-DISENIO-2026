package ar.utn.donatrack.donaciones.interfaces.repositories;

import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import java.util.List;
import java.util.UUID;

public interface EntidadesBeneficiariasRepositoryInterface {
    void guardar(EntidadBeneficiaria entidad);
    List<EntidadBeneficiaria> buscarTodas();
    EntidadBeneficiaria obtenerPorId(UUID id);
    boolean existePorId(UUID id);
    void eliminar(UUID id);
}