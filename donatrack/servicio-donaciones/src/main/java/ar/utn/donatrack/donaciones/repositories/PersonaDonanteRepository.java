package ar.utn.donatrack.donaciones.repositories;

import ar.utn.donatrack.donaciones.interfaces.repositories.PersonaDonanteRepositoryInterface;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.donante.PersonaJuridica;
import ar.utn.donatrack.donaciones.models.donante.Representante;
import ar.utn.donatrack.donaciones.repositories.jpa.PersonaDonanteJpaRepository;
import ar.utn.donatrack.donaciones.util.NormalizadorEmail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Persistencia de personas donantes en base relacional (Entrega 4).
 *
 * Reemplaza al ConcurrentHashMap con índice por email que había antes. Desde la
 * Entrega 5 la unicidad del email la garantiza un UNIQUE de la base, y la
 * búsqueda por email usa el índice que crea esa restricción.
 *
 * Todo email que entra por esta clase se normaliza antes de consultar: así el
 * resto del servicio puede pasar el email tal como lo escribió la persona.
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
        return email != null && jpa.existsByEmail(NormalizadorEmail.normalizar(email));
    }

    @Transactional(readOnly = true)
    public PersonaDonante obtenerPorEmail(String email) {
        return email == null ? null : jpa.findByEmail(NormalizadorEmail.normalizar(email)).orElse(null);
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
