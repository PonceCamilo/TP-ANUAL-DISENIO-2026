package ar.utn.donatrack.donaciones.interfaces.repositories;

import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.models.entidad.Necesidad;

import java.util.List;

public interface EntidadesBeneficiariasRepositoryInterface {
    void cargarNecesidad(EntidadBeneficiaria entidad, Necesidad necesidad);
    void guardar(EntidadBeneficiaria entidad);
    List<EntidadBeneficiaria> buscarTodas();
}