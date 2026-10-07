package ar.utn.donatrack.donaciones.repositories;

import ar.utn.donatrack.donaciones.interfaces.repositories.EntidadesBeneficiariasRepositoryInterface;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.repositories.jpa.EntidadBeneficiariaJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public void guardar(EntidadBeneficiaria entidad) {
        if (entidad.getId() == null) {
            entidad.setId(UUID.randomUUID());
        }
        jpa.save(entidad);
    }

    @Transactional(readOnly = true)
    public List<EntidadBeneficiaria> buscarTodas() {
        return jpa.findAll();
    }

    @Transactional(readOnly = true)
    public EntidadBeneficiaria obtenerPorId(UUID id) {
        return jpa.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public boolean existePorId(UUID id) {
        return jpa.existsById(id);
    }

    @Transactional
    public void eliminar(UUID id) {
        jpa.deleteById(id);
    }
}
