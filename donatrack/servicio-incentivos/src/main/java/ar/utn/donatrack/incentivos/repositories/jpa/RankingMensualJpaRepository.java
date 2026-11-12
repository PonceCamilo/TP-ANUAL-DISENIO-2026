package ar.utn.donatrack.incentivos.repositories.jpa;

import ar.utn.donatrack.incentivos.models.RankingMensual;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RankingMensualJpaRepository extends JpaRepository<RankingMensual, Long> {
    Optional<RankingMensual> findByPeriodoInicio(LocalDateTime periodoInicio);

    boolean existsByPeriodoInicio(LocalDateTime periodoInicio);

    // usamos querys para traer solo los periodos y puestos que necesitamos.
    @Query("select distinct r.periodoInicio from RankingMensual r order by r.periodoInicio desc")
    List<LocalDateTime> listarPeriodos();

    @Query("""
            select posicion.puesto as puesto,
                   posicion.donante.id as donanteId,
                   posicion.misionesCompletadas as misionesCompletadas
            from RankingMensual ranking
            join ranking.posiciones posicion
            where ranking.periodoInicio = :inicio
            order by posicion.puesto
            """)
    Page<PuestoGuardado> listarPuestos(@Param("inicio") LocalDateTime inicio, Pageable pagina);

    interface PuestoGuardado {
        int getPuesto();
        UUID getDonanteId();
        int getMisionesCompletadas();
    }
}
