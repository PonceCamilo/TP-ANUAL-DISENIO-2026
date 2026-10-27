package ar.utn.donatrack.donaciones.interfaces.repositories;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Lectura del contador desnormalizado de donaciones recibidas por entidad
 * (ver RecepcionDiariaEntidad). Lo usa el algoritmo de prioridad a
 * sub-atendidos; quien lo mantiene al día es DonacionesRepository.
 */
public interface RecepcionesEntidadRepositoryInterface {

    /**
     * Donaciones asignadas a cada entidad desde la fecha indicada, inclusive.
     * Las entidades que no recibieron nada no aparecen en el mapa.
     */
    Map<UUID, Long> contarRecepcionesDesde(LocalDate desde);

    /** Lo mismo para una sola entidad. */
    long contarRecepcionesDesde(UUID idEntidad, LocalDate desde);

    /**
     * Recalcula el contador entero desde la tabla donacion. Devuelve la cantidad
     * de filas (entidad, día) que quedaron.
     */
    int reconciliar();
}
