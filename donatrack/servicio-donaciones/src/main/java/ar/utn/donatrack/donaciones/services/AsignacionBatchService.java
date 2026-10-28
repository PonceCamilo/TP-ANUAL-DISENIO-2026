package ar.utn.donatrack.donaciones.services;

import java.util.Optional;
import java.util.ArrayList;
import ar.utn.donatrack.donaciones.models.asignacion.ResultadoMatchmaking;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioCandidatosAsignacion;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioDonaciones;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioEntidadesBeneficiarias;
import ar.utn.donatrack.donaciones.models.asignacion.RankingPrecalculado;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static ar.utn.donatrack.donaciones.models.donacion.estado.EstadosDonacion.EN_DEPOSITO;

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

    private final RepositorioDonaciones donacionesRepositorio;
    private final AsignacionDonacionesService asignacionService;
    private final RepositorioCandidatosAsignacion candidatosRepositorio;
    private final RepositorioEntidadesBeneficiarias entidadesRepositorio;

    /**
     * Corre todos los días a las 03:30 de Argentina, que es la franja de menor
     * uso del depósito. La zona se declara explícitamente: sin ella el horario
     * dependía de la zona del servidor (UTC en un contenedor).
     */
    @Scheduled(cron = "${donaciones.procesos.cron-matchmaking:0 30 3 * * *}", zone = FechaHoraArgentina.ZONA_ID)
    @Transactional
    public void ejecutarMatchmakingNocturno() {
        List<Donacion> enDeposito = donacionesRepositorio.obtenerPorEstado(EN_DEPOSITO);

        // Una donación con bienes vencidos no se le recomienda a nadie: queda
        // fuera de la bandeja hasta que la persona administradora la pase a
        // VENCIDA (GET /donaciones?vencidas=true las lista).
        LocalDate hoy = FechaHoraArgentina.hoy();
        List<Donacion> conVencidos = enDeposito.stream().filter(d -> d.tieneBienesVencidosAl(hoy)).toList();
        if (!conVencidos.isEmpty()) {
            log.warn("[ASIGNACION] {} donaciones en depósito tienen bienes vencidos y quedan fuera del matchmaking: {}",
                    conVencidos.size(), conVencidos.stream().map(Donacion::getId).toList());
            enDeposito = enDeposito.stream().filter(d -> !d.tieneBienesVencidosAl(hoy)).toList();
        }

        log.info("[ASIGNACION] Inicio del matchmaking nocturno. Donaciones en depósito: {}", enDeposito.size());

        // Las entidades son las mismas para todas las donaciones de la corrida:
        // se cargan una vez y no una vez por donación (Entrega 5).
        List<EntidadBeneficiaria> entidades = entidadesRepositorio.buscarTodas();

        List<RankingPrecalculado> rankings = new ArrayList<>();
        for (Donacion donacion : enDeposito) {
            calcularRanking(donacion, entidades).ifPresent(rankings::add);
        }

        // Los rankings de la corrida anterior se descartan (las necesidades de las
        // entidades pudieron cambiar durante el día y una recomendación vieja es
        // peor que ninguna), pero recién al final y de una vez: antes se vaciaba
        // la bandeja al principio y, si el proceso se cortaba, quedaba a medias.
        candidatosRepositorio.reemplazarTodos(rankings);

        long conCoincidencias = rankings.stream().filter(RankingPrecalculado::huboCoincidencias).count();
        long sinCandidatas = rankings.stream().filter(RankingPrecalculado::sinCandidatas).count();
        log.info("[ASIGNACION] Matchmaking finalizado. Procesadas: {} | Con coincidencia entre todos los algoritmos: {} | Sin ninguna candidata: {}",
                enDeposito.size(), conCoincidencias, sinCandidatas);
    }

    /**
     * Calcula el ranking de una donación aislando los errores: si una donación
     * rompe (datos incompletos, entidad borrada a mitad de camino), se registra y
     * el proceso sigue con las demás. Es un batch nocturno sin nadie mirando, así
     * que no puede caerse entero por un caso puntual.
     */
    private Optional<RankingPrecalculado> calcularRanking(Donacion donacion, List<EntidadBeneficiaria> entidades) {
        try {
            ResultadoMatchmaking resultado = asignacionService.generarRanking(donacion, entidades);
            return Optional.of(RankingPrecalculado.de(donacion.getId(), resultado, FechaHoraArgentina.ahora()));
        } catch (Exception e) {
            log.error("[ASIGNACION] No se pudo calcular el ranking de la donación {}: {}",
                    donacion.getId(), e.getMessage());
            return Optional.empty();
        }
    }
}
