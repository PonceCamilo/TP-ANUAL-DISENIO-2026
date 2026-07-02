package ar.utn.donatrack.incentivos.repositories;

import ar.utn.donatrack.incentivos.interfaces.repositories.RankingMensualRepositoryInterface;
import ar.utn.donatrack.incentivos.models.RankingMensual;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
@Repository
public class RankingMensualRepository implements RankingMensualRepositoryInterface {

    private final List<RankingMensual> historial = new ArrayList<>();

    public void guardar(RankingMensual ranking) {
        historial.add(ranking);
    }

    public Optional<RankingMensual> buscarPorPeriodo(int mes, int anio) {
        return historial.stream()
                .filter(r -> r.getMes() == mes && r.getAnio() == anio)
                .findFirst();
    }

    public List<RankingMensual> obtenerHistorial() {
        return historial;
    }
}
