package ar.utn.donatrack.donaciones.repositories;

import ar.utn.donatrack.donaciones.interfaces.repositories.EntidadesBeneficiariasRepositoryInterface;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Necesidad;

import lombok.Getter;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Repository
@Getter
public class EntidadesBeneficiariasRepository implements EntidadesBeneficiariasRepositoryInterface {

    private final List<EntidadBeneficiaria> entidadesBeneficiarias =
        Collections.synchronizedList(new ArrayList<>());

    public void guardar(EntidadBeneficiaria entidad) {
        // Si ya existe (misma UUID), reemplazar; si no, agregar
        entidadesBeneficiarias.removeIf(e -> e.getId().equals(entidad.getId()));
        entidadesBeneficiarias.add(entidad);
    }

    public void cargarNecesidad(EntidadBeneficiaria entidad, Necesidad necesidad) {
        entidad.registrarNecesidad(necesidad);
    }

    public List<EntidadBeneficiaria> buscarTodas() {
        return entidadesBeneficiarias;
    }

    public EntidadBeneficiaria buscarPorId(UUID id) {
        return entidadesBeneficiarias.stream()
            .filter(e -> e.getId().equals(id))
            .findFirst()
            .orElse(null);
    }
}
