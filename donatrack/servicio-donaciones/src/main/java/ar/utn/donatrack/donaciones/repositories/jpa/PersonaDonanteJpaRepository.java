package ar.utn.donatrack.donaciones.repositories.jpa;

import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Acceso JPA a la jerarquía persona_donante / persona_humana / persona_juridica.
 *
 * La búsqueda por email reemplaza al índice en memoria que mantenía el
 * repositorio anterior: ahora el índice es un UNIQUE de la base, que además
 * garantiza la unicidad de verdad y no solo mientras el proceso esté vivo.
 */
public interface PersonaDonanteJpaRepository extends JpaRepository<PersonaDonante, UUID> {

    Optional<PersonaDonante> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<PersonaDonante> findByEstadoNombre(String estadoNombre);
}
