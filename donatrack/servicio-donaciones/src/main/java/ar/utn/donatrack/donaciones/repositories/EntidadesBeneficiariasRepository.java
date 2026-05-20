package ar.utn.donatrack.donaciones.repositories;

import ar.utn.donatrack.donaciones.model.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.model.entidad.Necesidad;
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

    private static EntidadesBeneficiariasRepository instance = null;
    private EntidadesBeneficiariasRepository(List<EntidadBeneficiaria> entidadesBeneficiarias) {
        this.entidadesBeneficiarias = entidadesBeneficiarias;
    }

    private List<EntidadBeneficiaria> entidadesBeneficiarias;
    
    public void guardar(EntidadBeneficiaria entidad) {
    this.entidadesBeneficiarias.add(entidad);
    }

    public static EntidadesBeneficiariasRepository instance() {
        if (instance == null) {
            instance = new EntidadesBeneficiariasRepository(new ArrayList<>());
        }
        return instance;
    }

    public void cargarNecesidad(EntidadBeneficiaria entidad, Necesidad necesidad) {
        entidad.registrarNecesidad(necesidad);
    }

    public List<EntidadBeneficiaria> buscarTodas() {
        return this.entidadesBeneficiarias;
    }

    
}