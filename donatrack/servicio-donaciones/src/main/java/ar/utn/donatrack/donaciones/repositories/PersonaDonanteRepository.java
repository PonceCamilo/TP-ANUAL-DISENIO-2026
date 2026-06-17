package ar.utn.donatrack.donaciones.repositories;

import ar.utn.donatrack.donaciones.excepcion.PersonaDonanteNoEncontradaException;
import ar.utn.donatrack.donaciones.interfaces.repositories.PersonaDonanteRepositoryInterface;
import ar.utn.donatrack.donaciones.models.donante.EstadoDonante;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// ═══════════════════════════════════════════════════════════════════════════════
// PATRÓN: SINGLETON
// ───────────────────────────────────────────────────────────────────────────────
// Por qué: el almacenamiento en memoria ES el estado de la aplicación mientras
// no hay base de datos real. Si se crearan dos instancias de este repositorio,
// cada una tendría su propio Map y los datos quedarían partidos: un request
// guardaría en una instancia y otro no los encontraría.
//
// Spring ya garantiza scope singleton con @Repository, pero lo hacemos explícito
// documentando el patrón para que quede claro en el diagrama de clases y en la
// justificación de diseño de la entrega.
//
// Beneficio: una única fuente de verdad en memoria durante toda la vida del proceso.
// ═══════════════════════════════════════════════════════════════════════════════


@Repository  // ← SINGLETON: Spring instancia esta clase una sola vez en todo el contexto
@Getter
@Setter
public class PersonaDonanteRepository implements PersonaDonanteRepositoryInterface {

    // el Map es el estado compartido de toda la aplicación en Entrega 1
    private Map<UUID, PersonaDonante> almacenamiento = new ConcurrentHashMap<>();

    public void guardar(PersonaDonante personaDonante) {
        almacenamiento.put(personaDonante.getId(), personaDonante);
    }
    
    public PersonaDonante obtenerPorId(UUID id) {
        return almacenamiento.get(id);
    }

    public PersonaDonante obtenerPorMail(String email) {
        return almacenamiento.values().stream()
            .filter(p -> p.getEmail().equalsIgnoreCase(email))
            .findFirst()
            .orElse(null);
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
        PersonaDonante persona = this.obtenerPorId(id);

        if (persona == null) throw new PersonaDonanteNoEncontradaException(id);

        // (STATE): la transición se valida comparando estados
        if (persona.getEstado() == EstadoDonante.INACTIVO) {                // ← transición de estado
            throw new IllegalStateException("La persona donante ya se encuentra dada de baja.");
        }
        persona.setEstado(EstadoDonante.INACTIVO);                       // ← cambio de estado
    }

    public void reactivar(UUID id) {
        PersonaDonante persona = this.obtenerPorId(id);

        if (persona == null) throw new PersonaDonanteNoEncontradaException(id);

        // STATE: transición inversa explícita
        if (persona.getEstado() == EstadoDonante.ACTIVO) {                // ← transición de estado
            throw new IllegalStateException("La persona donante ya se encuentra activa.");
        }
        persona.setEstado(EstadoDonante.ACTIVO);                         // ← cambio de estado
    }
}
