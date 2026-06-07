package ar.utn.donatrack.donaciones.interfaces.services;

import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Campania;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Necesidad;

public interface EntidadesBeneficiariasServiceInterface {
    void registrarNecesidad(EntidadBeneficiaria entidad, Necesidad necesidad);
    void registrarNecesidades(EntidadBeneficiaria entidad, Campania campania);
    void actualizarPeriodos();
}
