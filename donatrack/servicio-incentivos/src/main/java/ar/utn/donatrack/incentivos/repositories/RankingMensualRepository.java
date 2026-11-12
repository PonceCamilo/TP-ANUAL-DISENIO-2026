package ar.utn.donatrack.incentivos.repositories;

import ar.utn.donatrack.incentivos.interfaces.repositories.RankingMensualRepositoryInterface;
import ar.utn.donatrack.incentivos.dtos.response.PuestoRankingResponse;
import ar.utn.donatrack.incentivos.models.RankingMensual;
import ar.utn.donatrack.incentivos.repositories.jpa.RankingMensualJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    public List<YearMonth> listarPeriodos() {
        return rankingMensualJpaRepository.listarPeriodos().stream().map(YearMonth::from).toList();
    }

    public Optional<Page<PuestoRankingResponse>> buscarPuestosGuardados(YearMonth periodo, Pageable pagina) {
        LocalDateTime inicio = periodo.atDay(1).atStartOfDay();
        if (!rankingMensualJpaRepository.existsByPeriodoInicio(inicio)) {
            return Optional.empty();
        }
        return Optional.of(rankingMensualJpaRepository.listarPuestos(inicio, pagina)
                .map(puesto -> new PuestoRankingResponse(
                        puesto.getPuesto(), puesto.getDonanteId(), puesto.getMisionesCompletadas())));
    }
}
