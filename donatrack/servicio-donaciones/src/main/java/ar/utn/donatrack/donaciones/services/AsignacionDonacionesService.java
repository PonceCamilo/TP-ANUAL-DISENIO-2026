package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.interfaces.repositories.EntidadesBeneficiariasRepositoryInterface;
import ar.utn.donatrack.donaciones.models.asignacion.ResultadoAsignacion;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.services.AlgoritmoCompatibilidadSemantica;
import ar.utn.donatrack.donaciones.services.AlgoritmoPrioridadSubAtendidos;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Orquesta el proceso de matchmaking de una donación: ejecuta ambos
 * algoritmos de asignación, intersecta sus rankings y devuelve el
 * resultado para que la persona administradora confirme el destino final.
 *
 * Si ambos algoritmos coinciden en alguna entidad, se devuelven solo
 * esas (mayor confianza). Si no hubo coincidencias, se devuelven
 * ambos rankings completos por separado, tal como pide el enunciado.
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

        if (!coincidencias.isEmpty()) {
            return new ResultadoMatchmaking(coincidencias, List.of(), List.of(), true);
        }
        return new ResultadoMatchmaking(List.of(), rankingSemantico, rankingSubAtendidos, false);
    }

    private List<ResultadoAsignacion> intersectar(List<ResultadoAsignacion> a, List<ResultadoAsignacion> b) {
        Set<java.util.UUID> idsB = b.stream().map(ResultadoAsignacion::getIdEntidad).collect(Collectors.toSet());
        return a.stream()
                .filter(resultado -> idsB.contains(resultado.getIdEntidad()))
                .toList();
    }

    @Getter
    public static class ResultadoMatchmaking {
        private final List<ResultadoAsignacion> coincidencias;
        private final List<ResultadoAsignacion> rankingSemantico;
        private final List<ResultadoAsignacion> rankingSubAtendidos;
        private final boolean huboCoincidencias;

        public ResultadoMatchmaking(List<ResultadoAsignacion> coincidencias,
                                    List<ResultadoAsignacion> rankingSemantico,
                                    List<ResultadoAsignacion> rankingSubAtendidos,
                                    boolean huboCoincidencias) {
            this.coincidencias = coincidencias;
            this.rankingSemantico = rankingSemantico;
            this.rankingSubAtendidos = rankingSubAtendidos;
            this.huboCoincidencias = huboCoincidencias;
        }
    }
}