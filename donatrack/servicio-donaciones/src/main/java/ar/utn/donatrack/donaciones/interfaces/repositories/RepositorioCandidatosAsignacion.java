package ar.utn.donatrack.donaciones.interfaces.repositories;

import ar.utn.donatrack.donaciones.models.asignacion.RankingPrecalculado;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Almacena el resultado del matchmaking que corre el proceso nocturno
 * (AsignacionBatchService), para que la persona administradora pueda confirmar
 * el destino final sin esperar a que los algoritmos se ejecuten otra vez.
 *
 * Desde la Entrega 5 la implementación es JPA (tablas ranking_*). Hasta la
 * Entrega 4 era en memoria; la interfaz permitió cambiarla sin tocar a quienes
 * la usan.
 */
public interface RepositorioCandidatosAsignacion {

    /**
     * Reemplaza la bandeja entera por los rankings de una corrida, en la
     * transacción de quien llama (el proceso nocturno): quien consulta ve la
     * bandeja anterior o la nueva completa, nunca una a medio armar.
     */
    void reemplazarTodos(Collection<RankingPrecalculado> rankings);

    /** Devuelve todos los rankings pendientes de confirmación. */
    List<RankingPrecalculado> obtenerTodos();

    /** Descarta el ranking de una donación (p. ej. cuando ya fue asignada). */
    void eliminar(UUID idDonacion);
}
