package ar.utn.donatrack.donaciones.interfaces.services;

import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Necesidad;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.NecesidadRecurrente;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.CargaNecesidad;

public interface EntidadesBeneficiariasServiceInterface {
    void registrarNecesidades(EntidadBeneficiaria entidad, CargaNecesidad carga);
    // void reiniciarPeriodoNecesidadRecurrente(NecesidadRecurrente necesidad);
    void actualizarPeriodos();
}


