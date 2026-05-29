package ar.utn.donatrack.donaciones.controllers;

import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.services.EntidadesBeneficiariasService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Campania;


@RestController
@RequiredArgsConstructor
public class EntidadBeneficiariaController {

    private final EntidadesBeneficiariasService entidadesBeneficiariasService;
    
    // La entidad beneficiaria debe poder registrar necesidades materiales concretas
    public void cargarNecesidades(EntidadBeneficiaria entidadBeneficiaria, Campania carga) {
        entidadesBeneficiariasService.registrarNecesidades(entidadBeneficiaria, carga);
    }
}
