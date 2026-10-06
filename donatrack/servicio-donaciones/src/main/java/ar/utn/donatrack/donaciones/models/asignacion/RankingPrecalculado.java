package ar.utn.donatrack.donaciones.models.asignacion;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Resultado del matchmaking de UNA donación, calculado por el proceso nocturno.
 *
 * Guarda las tres listas que devuelven los algoritmos (el ranking de cada uno y
 * la intersección) junto con el momento del cálculo, para que la persona
 * administradora sepa qué tan vigente es la recomendación que está viendo.
 */
@Getter
@Builder
public class RankingPrecalculado {

    private final UUID idDonacion;
    private final List<ResultadoAsignacion> coincidencias;
    private final List<ResultadoAsignacion> rankingSemantico;
    private final List<ResultadoAsignacion> rankingSubAtendidos;
    private final LocalDateTime fechaCalculo;

    /** Las coincidencias son las candidatas de mayor confianza: las recomiendan ambos algoritmos. */
    public boolean huboCoincidencias() {
        return coincidencias != null && !coincidencias.isEmpty();
    }

    /** True si ningún algoritmo encontró una sola entidad candidata. */
    public boolean sinCandidatas() {
        return (rankingSemantico == null || rankingSemantico.isEmpty())
                && (rankingSubAtendidos == null || rankingSubAtendidos.isEmpty());
    }
}
