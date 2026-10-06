package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.interfaces.repositories.CandidatosAsignacionRepositoryInterface;
import ar.utn.donatrack.donaciones.interfaces.repositories.DonacionesRepositoryInterface;
import ar.utn.donatrack.donaciones.models.asignacion.RankingPrecalculado;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Ejecuta el matchmaking de todas las donaciones que están EN_DEPOSITO.
 *
 * El enunciado (Entrega 2) pide dos cosas sobre este proceso:
 *  - "Por cada una de las donaciones cuyo estado sea En Depósito, el sistema
 *    debe ejecutar un proceso de matchmaking".
 *  - "Para asegurarse de que la ejecución de los algoritmos no degrade el
 *    desempeño del sistema, se solicita que el mismo se realice en horarios de
 *    baja carga."
 *
 * Por eso corre de madrugada y no en el camino de la request.
 *
 * DECISIÓN DE DISEÑO: el proceso NO asigna por su cuenta. Calcula los rankings y
 * los deja guardados para que una persona administradora confirme el destino
 * final, tal como pide el enunciado ("...para que una persona administradora
 * confirme el destino final"). La asignación efectiva sigue pasando por
 * DonacionService.asignar(), que es el único punto donde una donación cambia a
 * ASIGNACION_REALIZADA.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsignacionBatchService {

    /** Estado de las donaciones que todavía esperan destino. */
    private static final String ESTADO_EN_DEPOSITO = "EN_DEPOSITO";

    private final DonacionesRepositoryInterface donacionesRepositorio;
    private final AsignacionDonacionesService asignacionService;
    private final CandidatosAsignacionRepositoryInterface candidatosRepositorio;

    /**
     * Corre todos los días a las 03:30 (hora del servidor), que es la franja de
     * menor uso del depósito.
     */
    @Scheduled(cron = "0 30 3 * * *")
    public void ejecutarMatchmakingNocturno() {
        List<Donacion> enDeposito = donacionesRepositorio.obtenerPorEstado(ESTADO_EN_DEPOSITO);

        log.info("[ASIGNACION] Inicio del matchmaking nocturno. Donaciones en depósito: {}", enDeposito.size());

        // Se descartan los rankings de la corrida anterior: las necesidades de las
        // entidades pudieron cambiar durante el día y una recomendación vieja es
        // peor que ninguna.
        candidatosRepositorio.limpiar();

        int conCoincidencias = 0;
        int sinCandidatas = 0;

        for (Donacion donacion : enDeposito) {
            RankingPrecalculado ranking = calcularRanking(donacion);
            if (ranking == null) {
                continue;
            }
            candidatosRepositorio.guardar(ranking);

            if (ranking.huboCoincidencias()) {
                conCoincidencias++;
            }
            if (ranking.sinCandidatas()) {
                sinCandidatas++;
            }
        }

        log.info("[ASIGNACION] Matchmaking finalizado. Procesadas: {} | Con coincidencia entre ambos algoritmos: {} | Sin ninguna candidata: {}",
                enDeposito.size(), conCoincidencias, sinCandidatas);
    }

    /**
     * Calcula el ranking de una donación aislando los errores: si una donación
     * rompe (datos incompletos, entidad borrada a mitad de camino), se registra y
     * el proceso sigue con las demás. Es un batch nocturno sin nadie mirando, así
     * que no puede caerse entero por un caso puntual.
     */
    private RankingPrecalculado calcularRanking(Donacion donacion) {
        try {
            AsignacionDonacionesService.ResultadoMatchmaking resultado =
                    asignacionService.generarRanking(donacion);

            return RankingPrecalculado.builder()
                    .idDonacion(donacion.getId())
                    .coincidencias(resultado.getCoincidencias())
                    .rankingSemantico(resultado.getRankingSemantico())
                    .rankingSubAtendidos(resultado.getRankingSubAtendidos())
                    .fechaCalculo(FechaHoraArgentina.ahora())
                    .build();

        } catch (Exception e) {
            log.error("[ASIGNACION] No se pudo calcular el ranking de la donación {}: {}",
                    donacion.getId(), e.getMessage());
            return null;
        }
    }
}
