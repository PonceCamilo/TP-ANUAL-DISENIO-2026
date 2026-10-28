package ar.utn.donatrack.donaciones.repositories.jpa;

import ar.utn.donatrack.donaciones.models.asignacion.RankingPrecalculado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

/**
 * Acceso JPA a la bandeja de rankings precalculados.
 *
 * Vaciar la bandeja se hace con dos DELETE directos (primero los puestos,
 * después la principal): borrarla entidad por entidad costaría una lectura y
 * varios borrados por cada donación.
 */
public interface RankingPrecalculadoJpaRepository extends JpaRepository<RankingPrecalculado, UUID> {

    @Modifying
    @Query(value = "DELETE FROM ranking_puesto", nativeQuery = true)
    void vaciarPuestos();

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM ranking_precalculado", nativeQuery = true)
    void vaciarRankings();
}
