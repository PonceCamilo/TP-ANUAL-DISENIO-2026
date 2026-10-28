package ar.utn.donatrack.donaciones.models.asignacion;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Resultado del matchmaking de una donación: el ranking de cada algoritmo, por
 * nombre y en el orden en que se ejecutaron.
 *
 * Las coincidencias no son la salida de un algoritmo sino un dato derivado: las
 * entidades que recomiendan todos los algoritmos a la vez, que son las
 * candidatas de mayor confianza. Antes había tres listas escritas a mano
 * (semántico, sub-atendidos y coincidencias): con un mapa, un algoritmo nuevo no
 * cambia esta clase.
 */
public record ResultadoMatchmaking(Map<String, List<ResultadoAsignacion>> rankings) {

    public ResultadoMatchmaking {
        Map<String, List<ResultadoAsignacion>> copia = new LinkedHashMap<>();
        rankings.forEach((algoritmo, ranking) -> copia.put(algoritmo, List.copyOf(ranking)));
        rankings = Collections.unmodifiableMap(copia);
    }

    /** El ranking de un algoritmo; vacío si no se ejecutó. */
    public List<ResultadoAsignacion> ranking(String algoritmo) {
        return rankings.getOrDefault(algoritmo, List.of());
    }

    /** Las entidades que aparecen en todos los rankings, en el orden del primero. */
    public List<ResultadoAsignacion> coincidencias() {
        if (rankings.isEmpty()) {
            return List.of();
        }
        Iterator<List<ResultadoAsignacion>> resto = rankings.values().iterator();
        List<ResultadoAsignacion> primero = resto.next();
        Set<UUID> comunes = new HashSet<>(ids(primero));
        resto.forEachRemaining(ranking -> comunes.retainAll(ids(ranking)));
        return primero.stream().filter(resultado -> comunes.contains(resultado.getIdEntidad())).toList();
    }

    public boolean huboCoincidencias() {
        return !coincidencias().isEmpty();
    }

    /** Ningún algoritmo encontró una sola entidad candidata. */
    public boolean sinCandidatas() {
        return rankings.values().stream().allMatch(List::isEmpty);
    }

    private static Set<UUID> ids(List<ResultadoAsignacion> ranking) {
        return ranking.stream().map(ResultadoAsignacion::getIdEntidad).collect(Collectors.toSet());
    }
}
