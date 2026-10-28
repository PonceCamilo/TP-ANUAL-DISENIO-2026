package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.config.DonacionesProperties;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioEntidadesBeneficiarias;
import ar.utn.donatrack.donaciones.interfaces.services.AlgoritmoAsignacion;
import ar.utn.donatrack.donaciones.models.asignacion.ResultadoAsignacion;
import ar.utn.donatrack.donaciones.models.asignacion.ResultadoMatchmaking;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Orquesta el matchmaking de una donación: ejecuta todos los algoritmos de
 * asignación registrados (los beans de AlgoritmoAsignacion, en su @Order) y
 * devuelve el ranking de cada uno por su nombre. Las coincidencias entre todos,
 * que son las candidatas de mayor confianza, las calcula ResultadoMatchmaking.
 *
 * No conoce los algoritmos concretos: antes recibía los dos por su clase y tenía
 * las tres listas escritas a mano. Sumar un algoritmo no toca esta clase.
 */
@Service
public class AsignacionDonacionesService {

    private final List<AlgoritmoAsignacion> algoritmos;
    private final RepositorioEntidadesBeneficiarias entidadesRepository;
    private final int tamanioRanking;

    public AsignacionDonacionesService(List<AlgoritmoAsignacion> algoritmos,
                                       RepositorioEntidadesBeneficiarias entidadesRepository,
                                       DonacionesProperties propiedades) {
        Set<String> nombres = new HashSet<>();
        for (AlgoritmoAsignacion algoritmo : algoritmos) {
            if (!nombres.add(algoritmo.nombre())) {
                throw new IllegalStateException("Hay dos algoritmos de asignación llamados " + algoritmo.nombre());
            }
        }
        this.algoritmos = List.copyOf(algoritmos);
        this.entidadesRepository = entidadesRepository;
        this.tamanioRanking = propiedades.matchmaking().tamanioRanking();
    }

    /** Ranking a demanda de una sola donación: carga las entidades y evalúa. */
    public ResultadoMatchmaking generarRanking(Donacion donacion) {
        return generarRanking(donacion, entidadesRepository.buscarTodas());
    }

    /**
     * Ranking contra un conjunto de entidades ya cargado. Lo usa el proceso
     * nocturno (Entrega 5): carga las entidades una sola vez para todas las
     * donaciones en depósito, en lugar de una vez por donación.
     */
    public ResultadoMatchmaking generarRanking(Donacion donacion, List<EntidadBeneficiaria> entidades) {
        Map<String, List<ResultadoAsignacion>> rankings = new LinkedHashMap<>();
        for (AlgoritmoAsignacion algoritmo : algoritmos) {
            rankings.put(algoritmo.nombre(), algoritmo.evaluar(donacion, entidades, tamanioRanking));
        }
        return new ResultadoMatchmaking(rankings);
    }
}
