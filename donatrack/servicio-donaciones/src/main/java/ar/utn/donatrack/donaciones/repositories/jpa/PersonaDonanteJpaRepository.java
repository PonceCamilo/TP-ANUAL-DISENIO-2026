package ar.utn.donatrack.donaciones.repositories.jpa;

import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Acceso JPA a la jerarquía persona_donante / persona_humana / persona_juridica.
 *
 * Las búsquedas por email comparan por igualdad exacta contra la forma canónica
 * (ver NormalizadorEmail): así usan el índice que crea el UNIQUE de la columna.
 * Una comparación sin distinguir mayúsculas (lower(email) = ...) no podría usarlo.
 * Quien llama tiene que pasar el email ya normalizado.
 */
public interface PersonaDonanteJpaRepository extends JpaRepository<PersonaDonante, UUID> {

    Optional<PersonaDonante> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<PersonaDonante> findByEstadoNombre(String estadoNombre, Pageable pagina);

    List<PersonaDonante> findByUltimaInteraccionBeforeOrUltimaInteraccionIsNull(LocalDateTime limite);
}
