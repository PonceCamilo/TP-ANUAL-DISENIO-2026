package ar.utn.donatrack.donaciones.interfaces.services;

import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.models.entidad.Necesidad;
import ar.utn.donatrack.donaciones.models.entidad.NecesidadRecurrente;

public interface EntidadesBeneficiariasServiceInterface {
    void registrarNecesidad(EntidadBeneficiaria entidad, Necesidad necesidad);
    void reiniciarPeriodoNecesidadRecurrente(NecesidadRecurrente necesidad);
    void actualizarPeriodos();
}


