package ar.utn.donatrack.donaciones.repositories;

import ar.utn.donatrack.donaciones.interfaces.repositories.CandidatosAsignacionRepositoryInterface;
import ar.utn.donatrack.donaciones.models.asignacion.RankingPrecalculado;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class CandidatosAsignacionRepository implements CandidatosAsignacionRepositoryInterface {

    private final Map<UUID, RankingPrecalculado> almacenamiento = new ConcurrentHashMap<>();

    public void guardar(RankingPrecalculado ranking) {
        almacenamiento.put(ranking.getIdDonacion(), ranking);
    }

    public RankingPrecalculado obtenerPorDonacion(UUID idDonacion) {
        return almacenamiento.get(idDonacion);
    }

    public List<RankingPrecalculado> obtenerTodos() {
        return new ArrayList<>(almacenamiento.values());
    }

    public void eliminar(UUID idDonacion) {
        almacenamiento.remove(idDonacion);
    }

    public void limpiar() {
        almacenamiento.clear();
    }
}
