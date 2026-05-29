package ar.utn.donatrack.donaciones.repositories;

import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Necesidad;
import ar.utn.donatrack.donaciones.interfaces.repositories.EntidadesBeneficiariasRepositoryInterface;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Repository;

@Repository
@Getter
@Setter
public class EntidadesBeneficiariasRepository implements EntidadesBeneficiariasRepositoryInterface {

    private List<EntidadBeneficiaria> entidadesBeneficiarias = new ArrayList<>();
    
    public void guardar(EntidadBeneficiaria entidad) {
        this.entidadesBeneficiarias.add(entidad);
    }

    public void cargarNecesidad(EntidadBeneficiaria entidad, Necesidad necesidad) {
        entidad.registrarNecesidad(necesidad);
    }

    public List<EntidadBeneficiaria> buscarTodas() {
        return this.entidadesBeneficiarias;
    }
}