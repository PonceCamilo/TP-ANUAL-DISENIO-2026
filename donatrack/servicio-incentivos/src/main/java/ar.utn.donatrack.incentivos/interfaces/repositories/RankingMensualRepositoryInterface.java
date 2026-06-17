package ar.utn.donatrack.incentivos.interfaces.repositories;

public class RankingMensualRepositoryInterface {
    void guardar(RankingMensual ranking);
    Optional<RankingMensual> buscarPorPeriodo(int mes, int anio);
    List<RankingMensual> obtenerHistorial();
}
