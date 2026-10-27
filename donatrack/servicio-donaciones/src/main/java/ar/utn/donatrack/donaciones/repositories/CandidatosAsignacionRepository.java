package ar.utn.donatrack.donaciones.repositories;

import ar.utn.donatrack.donaciones.interfaces.repositories.CandidatosAsignacionRepositoryInterface;
import ar.utn.donatrack.donaciones.models.asignacion.RankingPrecalculado;
import ar.utn.donatrack.donaciones.repositories.jpa.RankingPrecalculadoJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Bandeja de rankings precalculados en base relacional (Entrega 5).
 *
 * Hasta la Entrega 4 era un ConcurrentHashMap: si el servicio se reiniciaba, la
 * bandeja quedaba vacía hasta la próxima corrida nocturna, y con más de una
 * instancia cada una tenía la suya. La interfaz no cambió, así que el proceso
 * nocturno y DonacionService no se enteraron del cambio.
 */
@Repository
@RequiredArgsConstructor
public class CandidatosAsignacionRepository implements CandidatosAsignacionRepositoryInterface {

    private final RankingPrecalculadoJpaRepository jpa;

    /** El id es el de la donación: guardar un ranking para la misma donación pisa el anterior. */
    public void guardar(RankingPrecalculado ranking) {
        jpa.save(ranking);
    }

    public RankingPrecalculado obtenerPorDonacion(UUID idDonacion) {
        return jpa.findById(idDonacion).orElse(null);
    }

    public List<RankingPrecalculado> obtenerTodos() {
        return jpa.findAll();
    }

    public void eliminar(UUID idDonacion) {
        jpa.deleteById(idDonacion);
    }

    public void limpiar() {
        jpa.vaciarCoincidencias();
        jpa.vaciarRankingSemantico();
        jpa.vaciarRankingSubAtendidos();
        jpa.vaciarRankings();
    }
}
