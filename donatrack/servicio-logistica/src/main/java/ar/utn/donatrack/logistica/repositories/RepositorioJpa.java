package ar.utn.donatrack.logistica.repositories;

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
}
