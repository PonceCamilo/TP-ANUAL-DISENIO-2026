package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.interfaces.repositories.EntidadesBeneficiariasRepositoryInterface;
import ar.utn.donatrack.donaciones.models.asignacion.ResultadoAsignacion;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Orquesta el matchmaking de una donación: ejecuta ambos algoritmos de
 * asignación y calcula la intersección de sus rankings.
 *
 * Siempre devuelve las tres listas (compatibilidad semántica, prioridad a
 * sub-atendidos y coincidencias entre ambos) para que la capa superior
 * decida cómo presentarlas. Las coincidencias son las candidatas de mayor
 * confianza: aparecen recomendadas por los dos criterios a la vez.
 */
@Service
@RequiredArgsConstructor
public class AsignacionDonacionesService {

    private final AlgoritmoCompatibilidadSemantica algoritmoSemantico;
    private final AlgoritmoPrioridadSubAtendidos algoritmoSubAtendidos;
    private final EntidadesBeneficiariasRepositoryInterface entidadesRepository;

    public ResultadoMatchmaking generarRanking(Donacion donacion) {
        List<EntidadBeneficiaria> entidades = entidadesRepository.buscarTodas();

        List<ResultadoAsignacion> rankingSemantico = algoritmoSemantico.evaluar(donacion, entidades);
        List<ResultadoAsignacion> rankingSubAtendidos = algoritmoSubAtendidos.evaluar(donacion, entidades);
        List<ResultadoAsignacion> coincidencias = intersectar(rankingSemantico, rankingSubAtendidos);

        return new ResultadoMatchmaking(coincidencias, rankingSemantico, rankingSubAtendidos);
    }

    private List<ResultadoAsignacion> intersectar(List<ResultadoAsignacion> a, List<ResultadoAsignacion> b) {
        Set<UUID> idsB = b.stream().map(ResultadoAsignacion::getIdEntidad).collect(Collectors.toSet());
        return a.stream()
                .filter(resultado -> idsB.contains(resultado.getIdEntidad()))
                .toList();
    }

    @Getter
    public static class ResultadoMatchmaking {
        private final List<ResultadoAsignacion> coincidencias;
        private final List<ResultadoAsignacion> rankingSemantico;
        private final List<ResultadoAsignacion> rankingSubAtendidos;

        public ResultadoMatchmaking(List<ResultadoAsignacion> coincidencias,
                                    List<ResultadoAsignacion> rankingSemantico,
                                    List<ResultadoAsignacion> rankingSubAtendidos) {
            this.coincidencias = coincidencias;
            this.rankingSemantico = rankingSemantico;
            this.rankingSubAtendidos = rankingSubAtendidos;
        }

        public boolean huboCoincidencias() {
            return !coincidencias.isEmpty();
        }
    }
}
