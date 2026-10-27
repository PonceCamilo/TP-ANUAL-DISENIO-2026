package ar.utn.donatrack.donaciones.repositories;

import ar.utn.donatrack.donaciones.interfaces.repositories.PersonaDonanteRepositoryInterface;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.donante.PersonaJuridica;
import ar.utn.donatrack.donaciones.models.donante.Representante;
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
public class PersonaDonanteRepository implements PersonaDonanteRepositoryInterface {

    /** El email es único: ordenar por él da páginas estables y legibles. */
    private static final Sort ORDEN_LISTADO = Sort.by("email");

    private final PersonaDonanteJpaRepository jpa;

    public void guardar(PersonaDonante personaDonante) {
        jpa.save(personaDonante);
    }

    public void guardarTodos(Collection<PersonaDonante> donantes) {
        jpa.saveAll(donantes);
    }

    public PersonaDonante obtenerPersona(UUID id) {
        return jpa.findById(id).orElse(null);
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

    public void modificarRepresentante(UUID idPersonaJuridica, Representante representante) {
        PersonaDonante persona = obtenerPersona(idPersonaJuridica);
        if (persona instanceof PersonaJuridica juridica) {
            juridica.agregarRepresentante(representante);
            jpa.save(juridica);
        }
    }

    public boolean existePorId(UUID id) {
        return jpa.existsById(id);
    }

    public boolean existePorEmail(String email) {
        return email != null && jpa.existsByEmail(NormalizadorEmail.normalizar(email));
    }

    public PersonaDonante obtenerPorEmail(String email) {
        return email == null ? null : jpa.findByEmail(NormalizadorEmail.normalizar(email)).orElse(null);
    }

    public Map<String, PersonaDonante> obtenerPorEmails(Collection<String> emails) {
        List<String> normalizados = emails.stream().map(NormalizadorEmail::normalizar).distinct().toList();
        if (normalizados.isEmpty()) {
            return Map.of();
        }
        return jpa.findByEmailIn(normalizados).stream()
                .collect(Collectors.toMap(PersonaDonante::getEmail, Function.identity()));
    }

    public void eliminar(UUID id) {
        jpa.deleteById(id);
    }
}
