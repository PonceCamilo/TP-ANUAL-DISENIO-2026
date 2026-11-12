package ar.utn.donatrack.incentivos.interfaces.repositories;
import ar.utn.donatrack.incentivos.models.RankingMensual;
import ar.utn.donatrack.incentivos.dtos.response.PuestoRankingResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface RankingMensualRepositoryInterface {
    void guardar(RankingMensual ranking);
    Optional<RankingMensual> buscarPorPeriodo(int mes, int anio);
    List<RankingMensual> obtenerHistorial();
    List<YearMonth> listarPeriodos();
    Optional<Page<PuestoRankingResponse>> buscarPuestosGuardados(YearMonth periodo, Pageable pagina);
}
