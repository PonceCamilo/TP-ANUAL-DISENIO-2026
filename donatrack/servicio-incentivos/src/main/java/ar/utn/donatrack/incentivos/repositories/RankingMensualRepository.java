package ar.utn.donatrack.incentivos.repositories;

import ar.utn.donatrack.incentivos.interfaces.repositories.RankingMensualRepositoryInterface;
import ar.utn.donatrack.incentivos.models.RankingMensual;
import ar.utn.donatrack.incentivos.repositories.jpa.RankingMensualJpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Repository
public class RankingMensualRepository implements RankingMensualRepositoryInterface {

    private final RankingMensualJpaRepository rankingMensualJpaRepository;

    public RankingMensualRepository(RankingMensualJpaRepository rankingMensualJpaRepository) {
        this.rankingMensualJpaRepository = rankingMensualJpaRepository;
    }

    public void guardar(RankingMensual ranking) {
        rankingMensualJpaRepository.save(ranking);
    }

    public Optional<RankingMensual> buscarPorPeriodo(int mes, int anio) {
        LocalDateTime periodoInicio = YearMonth.of(anio, mes).atDay(1).atStartOfDay();
        return rankingMensualJpaRepository.findByPeriodoInicio(periodoInicio);
    }

    public List<RankingMensual> obtenerHistorial() {
        return rankingMensualJpaRepository.findAll();
    }
}
