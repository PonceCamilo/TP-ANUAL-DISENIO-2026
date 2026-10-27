package ar.utn.donatrack.donaciones.repositories.jpa;

import ar.utn.donatrack.donaciones.models.asignacion.RankingPrecalculado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

/**
 * Acceso JPA a la bandeja de rankings precalculados.
 *
 * Vaciar la bandeja se hace con cuatro DELETE directos (primero las tablas de
 * las listas, después la principal): borrarla entidad por entidad costaría una
 * lectura y varios borrados por cada donación.
 */
public interface RankingPrecalculadoJpaRepository extends JpaRepository<RankingPrecalculado, UUID> {

    @Modifying
    @Query(value = "DELETE FROM ranking_coincidencia", nativeQuery = true)
    void vaciarCoincidencias();

    @Modifying
    @Query(value = "DELETE FROM ranking_semantico", nativeQuery = true)
    void vaciarRankingSemantico();

    @Modifying
    @Query(value = "DELETE FROM ranking_subatendidos", nativeQuery = true)
    void vaciarRankingSubAtendidos();

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM ranking_precalculado", nativeQuery = true)
    void vaciarRankings();
}
