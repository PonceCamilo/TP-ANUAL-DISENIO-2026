package ar.utn.donatrack.incentivos.interfaces.repositories;
import ar.utn.donatrack.incentivos.models.RankingMensual;

import java.util.List;
import java.util.Optional;

public class RankingMensualRepositoryInterface {
    void guardar(RankingMensual ranking);
    Optional<RankingMensual> buscarPorPeriodo(int mes, int anio);
    List<RankingMensual> obtenerHistorial();
}
