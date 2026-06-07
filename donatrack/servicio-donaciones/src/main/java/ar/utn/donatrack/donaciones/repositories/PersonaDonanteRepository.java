package ar.utn.donatrack.donaciones.repositories;

import ar.utn.donatrack.donaciones.exceptions.EmailYaRegistradoException;
import ar.utn.donatrack.donaciones.exceptions.PersonaDonanteNoEncontradaException;
import ar.utn.donatrack.donaciones.interfaces.repositories.PersonaDonanteRepositoryInterface;
import ar.utn.donatrack.donaciones.models.donante.EstadoDonante;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;

import lombok.Getter;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repositorio en memoria para PersonaDonante.
 * Spring garantiza scope singleton con @Repository: una única instancia
 * compartida en todo el contexto de la aplicación.
 *
 * El índice secundario `porEmail` permite O(1) para la búsqueda por email,
 * necesaria tanto en el registro individual como en la importación masiva CSV.
 */
@Repository
@Getter
public class PersonaDonanteRepository implements PersonaDonanteRepositoryInterface {

    private final Map<UUID, PersonaDonante> almacenamiento = new ConcurrentHashMap<>();

    // Índice secundario: email → UUID para búsqueda eficiente sin recorrer todo el mapa
    private final Map<String, UUID> indicePorEmail = new ConcurrentHashMap<>();

    /**
     * Persiste una nueva PersonaDonante.
     * Lanza EmailYaRegistradoException si el email ya está en uso.
     * Genera un UUID si la entidad no tiene uno asignado.
     */
    public void guardar(PersonaDonante personaDonante) {
        if (personaDonante.getId() == null) {
            personaDonante.setId(UUID.randomUUID());
        }

        String email = personaDonante.getEmail();
        if (email != null && indicePorEmail.containsKey(email.toLowerCase())) {
            // Solo lanzar si el ID es distinto (no es una actualización del mismo objeto)
            UUID idExistente = indicePorEmail.get(email.toLowerCase());
            if (!idExistente.equals(personaDonante.getId())) {
                throw new EmailYaRegistradoException();
            }
        }

        almacenamiento.put(personaDonante.getId(), personaDonante);
        if (email != null) {
            indicePorEmail.put(email.toLowerCase(), personaDonante.getId());
        }
    }

    public PersonaDonante obtenerPorId(UUID id) {
        return almacenamiento.get(id);
    }

    /**
     * Búsqueda O(1) por email usando el índice secundario.
     * Devuelve null si no existe (sin lanzar excepción, para uso en importación CSV).
     */

    public PersonaDonante obtenerPorMail(String email) {
        if (email == null) return null;
        UUID id = indicePorEmail.get(email.toLowerCase());
        return id != null ? almacenamiento.get(id) : null;
    }

    public List<PersonaDonante> obtenerTodosDonantes() {
        return almacenamiento.values().stream().toList();
    }

    public List<PersonaDonante> obtenerTodosActivos() {
        return almacenamiento.values().stream()
            .filter(p -> p.getEstado() == EstadoDonante.ACTIVO)
            .toList();
    }

    public void darDeBaja(UUID id) {
        PersonaDonante persona = obtenerPorId(id);
        if (persona == null) throw new PersonaDonanteNoEncontradaException(id);
        if (persona.getEstado() == EstadoDonante.INACTIVO) {
            throw new IllegalStateException("La persona donante ya se encuentra dada de baja.");
        }
        persona.setEstado(EstadoDonante.INACTIVO);
    }

    public void reactivar(UUID id) {
        PersonaDonante persona = obtenerPorId(id);
        if (persona == null) throw new PersonaDonanteNoEncontradaException(id);
        if (persona.getEstado() == EstadoDonante.ACTIVO) {
            throw new IllegalStateException("La persona donante ya se encuentra activa.");
        }
        persona.setEstado(EstadoDonante.ACTIVO);
    }
}
