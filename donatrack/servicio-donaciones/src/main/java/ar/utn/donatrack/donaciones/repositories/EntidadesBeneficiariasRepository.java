package ar.utn.donatrack.donaciones.repositories;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Repository;
import ar.utn.donatrack.donaciones.util.SolicitudPagina;
import ar.utn.donatrack.donaciones.util.Pagina;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioEntidadesBeneficiarias;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.repositories.jpa.EntidadBeneficiariaJpaRepository;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
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
public class EntidadesBeneficiariasRepository implements RepositorioEntidadesBeneficiarias {

    private static final Sort ORDEN_LISTADO = Sort.by(Sort.Order.asc("razonSocial"), Sort.Order.asc("id"));

    private final EntidadBeneficiariaJpaRepository jpa;

    public void guardar(EntidadBeneficiaria entidad) {
        jpa.save(entidad);
    }

    public List<EntidadBeneficiaria> buscarTodas() {
        return jpa.findAll();
    }

    public Pagina<EntidadBeneficiaria> buscar(SolicitudPagina pagina) {
        Page<EntidadBeneficiaria> resultado = jpa.findAll(PageRequest.of(pagina.numero(), pagina.tamanio(), ORDEN_LISTADO));
        return new Pagina<>(resultado.getContent(), pagina.numero(), pagina.tamanio(), resultado.getTotalElements());
    }

    public Optional<EntidadBeneficiaria> buscarPorId(UUID id) {
        return jpa.findById(id);
    }

    public List<EntidadBeneficiaria> obtenerPorIds(Collection<UUID> ids) {
        return jpa.findAllById(ids);
    }

    public void eliminar(EntidadBeneficiaria entidad) {
        jpa.delete(entidad);
    }
}
