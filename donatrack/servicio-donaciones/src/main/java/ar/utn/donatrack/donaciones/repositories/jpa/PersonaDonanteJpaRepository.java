package ar.utn.donatrack.donaciones.repositories.jpa;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
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

    List<PersonaDonante> findByEmailIn(Collection<String> emails);

    Page<PersonaDonante> findByEstadoNombre(String estadoNombre, Pageable pagina);

    /**
     * Personas a las que corresponde avisar por inactividad: sin interacción desde
     * el límite (o nunca), sin un aviso desde el límite y que no estén BLOQUEADAS.
     */
    @Query("SELECT p FROM PersonaDonante p WHERE p.estadoNombre <> 'BLOQUEADO' "
            + "AND (p.ultimaInteraccion IS NULL OR p.ultimaInteraccion < :limite) "
            + "AND (p.ultimoAvisoInactividad IS NULL OR p.ultimoAvisoInactividad < :limite)")
    List<PersonaDonante> buscarParaAvisoDeInactividad(@Param("limite") LocalDateTime limite, Pageable pagina);

    /** Marca el aviso de inactividad de un lote de personas con una sola sentencia. */
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE persona_donante SET ultimo_aviso_inactividad = :cuando WHERE id IN (:ids)", nativeQuery = true)
    int registrarAvisoDeInactividad(@Param("ids") Collection<UUID> ids, @Param("cuando") LocalDateTime cuando);
}
