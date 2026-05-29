package ar.utn.donatrack.donaciones.interfaces.services;

import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Campania;

public interface EntidadesBeneficiariasServiceInterface {
    void registrarNecesidades(EntidadBeneficiaria entidad, Campania carga);
    // void reiniciarPeriodoNecesidadRecurrente(NecesidadRecurrente necesidad);
    void actualizarPeriodos();
}


