package ar.utn.donatrack.donaciones.interfaces.services;

import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Campania;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Necesidad;

import java.util.List;
import java.util.UUID;

public interface EntidadesBeneficiariasServiceInterface {

    void guardar(EntidadBeneficiaria entidad);

    List<EntidadBeneficiaria> obtenerTodas();

    EntidadBeneficiaria obtenerPorId(UUID id);

    void actualizar(UUID id, EntidadBeneficiaria entidadNueva);

    void agregarNecesidadACampania(UUID idEntidad, UUID  idCampania, Necesidad necesidad);

    void actualizarPeriodos();
}
