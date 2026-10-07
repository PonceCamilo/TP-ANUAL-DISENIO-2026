package ar.utn.donatrack.logistica.repositories;

import ar.utn.donatrack.logistica.models.referencias.Donacion;
import ar.utn.donatrack.logistica.models.referencias.EntidadBeneficiaria;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.UUID;

/**
 * Base común de los repositorios JPA de logística.
 *
 * Los ids los asigna el dominio (UUID.randomUUID()), así que "guardar" decide
 * entre persist y merge: con persist el mismo objeto que usa el service queda
 * gestionado por JPA, y los cambios posteriores dentro de la transacción se
 * guardan solos (igual que pasaba con los repositorios en memoria, que
 * guardaban la referencia).
 */
abstract class RepositorioJpa<T> {

    @PersistenceContext
    protected EntityManager em;

    private final Class<T> tipo;

    protected RepositorioJpa(Class<T> tipo) {
        this.tipo = tipo;
    }

    protected void guardarEntidad(T entidad, UUID id) {
        if (em.contains(entidad)) {
            return;
        }
        if (em.find(tipo, id) == null) {
            em.persist(entidad);
        } else {
            em.merge(entidad);
        }
    }

    protected T buscarEntidad(UUID id) {
        return id == null ? null : em.find(tipo, id);
    }

    /**
     * Registra en las tablas de referencia (donacion, entidad_beneficiaria) los
     * ids de servicio-donaciones que usa logística, si todavía no están, para que
     * sus foreign keys sean válidas. Se llama antes de guardar lo que los referencia.
     */
    protected void registrarReferencias(UUID idDonacion, UUID idEntidadBeneficiaria) {
        if (idDonacion != null && em.find(Donacion.class, idDonacion) == null) {
            em.persist(new Donacion(idDonacion));
        }
        if (idEntidadBeneficiaria != null && em.find(EntidadBeneficiaria.class, idEntidadBeneficiaria) == null) {
            em.persist(new EntidadBeneficiaria(idEntidadBeneficiaria));
        }
    }
}
