package ar.utn.donatrack.donaciones.controller;

import ar.utn.donatrack.donaciones.model.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.model.entidad.Necesidad;
import ar.utn.donatrack.donaciones.services.EntidadesBeneficiariasService;

public class EntidadBeneficiariaController {
    private final EntidadesBeneficiariasService entidadesBeneficiariasService =EntidadesBeneficiariasService.instance();
    
    // La entidad beneficiaria debe poder registrar necesidades materiales concretas
    public void cargarNecesidad(EntidadBeneficiaria entidadBeneficiaria, Necesidad necesidad) {
        entidadesBeneficiariasService.registrarNecesidad(entidadBeneficiaria, necesidad);
    }
}
