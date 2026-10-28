package ar.utn.donatrack.donaciones.repositories;

import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioCandidatosAsignacion;
import ar.utn.donatrack.donaciones.models.asignacion.RankingPrecalculado;
import ar.utn.donatrack.donaciones.repositories.jpa.RankingPrecalculadoJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Bandeja de rankings precalculados en base relacional (Entrega 5).
 *
 * Hasta la Entrega 4 era un ConcurrentHashMap: si el servicio se reiniciaba, la
 * bandeja quedaba vacía hasta la próxima corrida nocturna, y con más de una
 * instancia cada una tenía la suya. La interfaz no cambió, así que el proceso
 * nocturno y los services de donaciones no se enteraron del cambio.
 */
@Repository
@RequiredArgsConstructor
public class CandidatosAsignacionRepository implements RepositorioCandidatosAsignacion {

    private final RankingPrecalculadoJpaRepository jpa;

    /** Dos DELETE directos y los inserts de la corrida nueva. */
    public void reemplazarTodos(Collection<RankingPrecalculado> rankings) {
        jpa.vaciarPuestos();
        jpa.vaciarRankings();
        jpa.saveAll(rankings);
    }

    public List<RankingPrecalculado> obtenerTodos() {
        return jpa.findAll();
    }

    public void eliminar(UUID idDonacion) {
        jpa.deleteById(idDonacion);
    }
}
