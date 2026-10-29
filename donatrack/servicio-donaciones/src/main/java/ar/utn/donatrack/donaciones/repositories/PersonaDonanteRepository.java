package ar.utn.donatrack.donaciones.repositories;

import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioPersonasDonantes;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.repositories.jpa.PersonaDonanteJpaRepository;
import ar.utn.donatrack.donaciones.util.NormalizadorEmail;
import ar.utn.donatrack.donaciones.util.Pagina;
import ar.utn.donatrack.donaciones.util.SolicitudPagina;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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
public class PersonaDonanteRepository implements RepositorioPersonasDonantes {

    /** El email es único: ordenar por él da páginas estables y legibles. */
    private static final Sort ORDEN_LISTADO = Sort.by("email");

    private final PersonaDonanteJpaRepository jpa;

    public void guardar(PersonaDonante personaDonante) {
        jpa.save(personaDonante);
    }

    public void guardarTodos(Collection<PersonaDonante> donantes) {
        jpa.saveAll(donantes);
    }

    public Optional<PersonaDonante> buscarPorId(UUID id) {
        return jpa.findById(id);
    }

    public List<PersonaDonante> obtenerPorIds(Collection<UUID> ids) {
        return ids.isEmpty() ? List.of() : jpa.findAllById(ids);
    }

    public Pagina<PersonaDonante> buscar(String estado, SolicitudPagina pagina) {
        PageRequest pedido = PageRequest.of(pagina.numero(), pagina.tamanio(), ORDEN_LISTADO);
        Page<PersonaDonante> resultado = (estado == null || estado.isBlank())
                ? jpa.findAll(pedido)
                : jpa.findByEstadoNombre(estado, pedido);
        return new Pagina<>(resultado.getContent(), pagina.numero(), pagina.tamanio(), resultado.getTotalElements());
    }

    public List<PersonaDonante> obtenerParaAvisoDeInactividad(LocalDateTime limite, int cantidad) {
        return jpa.buscarParaAvisoDeInactividad(limite, PageRequest.of(0, cantidad, Sort.by("id")));
    }

    public void registrarAvisoDeInactividad(Collection<UUID> ids, LocalDateTime cuando) {
        jpa.registrarAvisoDeInactividad(ids, cuando);
    }

    public boolean existePorEmail(String email) {
        return email != null && jpa.existsByEmail(NormalizadorEmail.normalizar(email));
    }

    public Optional<PersonaDonante> buscarPorEmail(String email) {
        return email == null ? Optional.empty() : jpa.findByEmail(NormalizadorEmail.normalizar(email));
    }

    public Map<String, PersonaDonante> obtenerPorEmails(Collection<String> emails) {
        List<String> normalizados = emails.stream().map(NormalizadorEmail::normalizar).distinct().toList();
        if (normalizados.isEmpty()) {
            return Map.of();
        }
        return jpa.findByEmailIn(normalizados).stream()
                .collect(Collectors.toMap(PersonaDonante::getEmail, Function.identity()));
    }

    public void eliminar(PersonaDonante persona) {
        jpa.delete(persona);
    }
}
