package ar.utn.donatrack.donaciones.interfaces.repositories;

import ar.utn.donatrack.donaciones.models.asignacion.RankingPrecalculado;

import java.util.List;
import java.util.UUID;

/**
 * Almacena el resultado del matchmaking que corre el proceso nocturno
 * (AsignacionBatchService), para que la persona administradora pueda confirmar
 * el destino final sin esperar a que los algoritmos se ejecuten otra vez.
 *
 * Igual que el resto de los repositorios del servicio, hoy la implementación es
 * en memoria; la interfaz existe para poder reemplazarla por una con JPA sin
 * tocar a quienes la usan.
 */
public interface CandidatosAsignacionRepositoryInterface {

    /** Guarda (o pisa) el ranking calculado para una donación. */
    void guardar(RankingPrecalculado ranking);

    /** Devuelve el ranking de una donación, o null si todavía no se calculó. */
    RankingPrecalculado obtenerPorDonacion(UUID idDonacion);

    /** Devuelve todos los rankings pendientes de confirmación. */
    List<RankingPrecalculado> obtenerTodos();

    /** Descarta el ranking de una donación (p. ej. cuando ya fue asignada). */
    void eliminar(UUID idDonacion);

    /** Vacía el almacenamiento antes de una nueva corrida del proceso nocturno. */
    void limpiar();
}
