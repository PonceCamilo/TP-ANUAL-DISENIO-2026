package ar.utn.donatrack.donaciones.repositories;

import ar.utn.donatrack.donaciones.interfaces.repositories.PersonaDonanteRepositoryInterface;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.donante.PersonaJuridica;
import ar.utn.donatrack.donaciones.models.donante.Representante;
import ar.utn.donatrack.donaciones.repositories.jpa.PersonaDonanteJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Persistencia de personas donantes en base relacional (Entrega 4).
 *
 * Reemplaza al ConcurrentHashMap con índice por email que había antes. Dos cosas
 * que mejoran con el cambio:
 *   - La unicidad del email la garantiza la base, no un Map que se vacía al
 *     reiniciar el proceso.
 *   - La búsqueda por email ya no depende de mantener un índice a mano y en
 *     sincronía con el almacenamiento principal.
 *
 * La interfaz no cambió, así que los services y sus tests siguen igual.
 */
@Repository
@RequiredArgsConstructor
public class PersonaDonanteRepository implements PersonaDonanteRepositoryInterface {

    private final PersonaDonanteJpaRepository jpa;

    @Transactional
    public void guardar(PersonaDonante personaDonante) {
        if (personaDonante.getId() == null) {
            personaDonante.setId(UUID.randomUUID());
        }
        jpa.save(personaDonante);
    }

    @Transactional(readOnly = true)
    public PersonaDonante obtenerPersona(UUID id) {
        return jpa.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<PersonaDonante> obtenerTodosDonantes() {
        return jpa.findAll();
    }

    @Transactional
    public void modificarRepresentante(UUID idPersonaJuridica, Representante representante) {
        PersonaDonante persona = obtenerPersona(idPersonaJuridica);
        if (persona instanceof PersonaJuridica juridica) {
            juridica.agregarRepresentante(representante);
            jpa.save(juridica);
        }
    }

    @Transactional(readOnly = true)
    public boolean existePorId(UUID id) {
        return jpa.existsById(id);
    }

    @Transactional(readOnly = true)
    public boolean existePorEmail(String email) {
        return email != null && jpa.existsByEmailIgnoreCase(email);
    }

    @Transactional(readOnly = true)
    public PersonaDonante obtenerPorEmail(String email) {
        return email == null ? null : jpa.findByEmailIgnoreCase(email).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<PersonaDonante> obtenerPorEstado(String estado) {
        return jpa.findByEstadoNombre(estado);
    }

    @Transactional
    public void eliminar(UUID id) {
        jpa.deleteById(id);
    }
}
