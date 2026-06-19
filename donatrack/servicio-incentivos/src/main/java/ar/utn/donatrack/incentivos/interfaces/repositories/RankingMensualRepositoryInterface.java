package ar.utn.donatrack.incentivos.interfaces.repositories;
import ar.utn.donatrack.incentivos.models.RankingMensual;

import java.util.List;
import java.util.Optional;

public interface RankingMensualRepositoryInterface {
    void guardar(RankingMensual ranking);
    Optional<RankingMensual> buscarPorPeriodo(int mes, int anio);
    List<RankingMensual> obtenerHistorial();
}
