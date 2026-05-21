package ar.utn.donatrack.donaciones.interfaces.services;

import ar.utn.donatrack.donaciones.model.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.model.entidad.Necesidad;

import java.util.List;

public interface EntidadesBeneficiariasServiceInterface {
    void registrarNecesidad(EntidadBeneficiaria entidad, Necesidad necesidad);
    void reiniciarPeriodoNecesidadRecurrente(EntidadBeneficiaria entidad, Necesidad necesidad);
    void actualizarPeriodos();
}


