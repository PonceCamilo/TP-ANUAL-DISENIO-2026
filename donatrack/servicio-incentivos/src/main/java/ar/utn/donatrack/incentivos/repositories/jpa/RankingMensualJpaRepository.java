package ar.utn.donatrack.incentivos.repositories.jpa;

import ar.utn.donatrack.incentivos.models.RankingMensual;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RankingMensualJpaRepository extends JpaRepository<RankingMensual, Long> {
    Optional<RankingMensual> findByPeriodoInicio(LocalDateTime periodoInicio);
}
