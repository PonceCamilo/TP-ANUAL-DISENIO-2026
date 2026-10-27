package ar.utn.donatrack.donaciones.repositories;

import ar.utn.donatrack.donaciones.interfaces.repositories.EntidadesBeneficiariasRepositoryInterface;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.repositories.jpa.EntidadBeneficiariaJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Persistencia de entidades beneficiarias en base relacional (Entrega 4).
 *
 * Guardar la entidad arrastra por cascada sus campañas y, a través de ellas, sus
 * necesidades. Por eso los services que agregan una necesidad terminan llamando
 * a guardar(entidad): la entidad es la raíz de la agregación y el único punto de
 * entrada para modificar cualquier cosa de su interior.
 */
@Repository
@RequiredArgsConstructor
public class EntidadesBeneficiariasRepository implements EntidadesBeneficiariasRepositoryInterface {

    private final EntidadBeneficiariaJpaRepository jpa;

    public void guardar(EntidadBeneficiaria entidad) {
        jpa.save(entidad);
    }

    public List<EntidadBeneficiaria> buscarTodas() {
        return jpa.findAll();
    }

    public EntidadBeneficiaria obtenerPorId(UUID id) {
        return jpa.findById(id).orElse(null);
    }

    public List<EntidadBeneficiaria> obtenerPorIds(Collection<UUID> ids) {
        return jpa.findAllById(ids);
    }

    public boolean existePorId(UUID id) {
        return jpa.existsById(id);
    }

    public void eliminar(UUID id) {
        jpa.deleteById(id);
    }
}
