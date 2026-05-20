package ar.utn.donatrack.donaciones.repositories;

import ar.utn.donatrack.donaciones.model.donante.EstadoDonante;
import ar.utn.donatrack.donaciones.model.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.model.contacto.TipoMedioContacto;
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
public class PersonaDonanteRepository{

    // el Map es el estado compartido de toda la aplicación en Entrega 1
    private final Map<UUID, PersonaDonante> almacenamiento = new ConcurrentHashMap<>(); 

    public void guardar(PersonaDonante personaDonante) {
        almacenamiento.put(personaDonante.getId(), personaDonante);
    }
    
    public Optional<PersonaDonante> buscarPorId(UUID id) {
        return Optional.ofNullable(almacenamiento.get(id));
    }

    public Optional<PersonaDonante> buscarPorEmail(String email) {
        return almacenamiento.values().stream()
                .filter(p -> p.getMediosDeContacto().stream()
                        .anyMatch(m -> m == TipoMedioContacto.EMAIL
                                && p.getEmail().equalsIgnoreCase(email)))
                .findFirst();
    }
    
    public List<PersonaDonante> buscarTodos() {
        return new ArrayList<>(almacenamiento.values());
    }
    
    public List<PersonaDonante> buscarActivos() {
        return almacenamiento.values().stream()
            .filter(p -> p.getEstado() == EstadoDonante.ACTIVO)
            .toList();
    }

    public void eliminar(UUID id) {
        almacenamiento.remove(id);
    }
    
    public boolean existePorEmail(String email) {
        return buscarPorEmail(email).isPresent();
    }
}
