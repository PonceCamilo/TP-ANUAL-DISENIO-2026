package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.config.DonacionesProperties;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioCandidatosAsignacion;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioDonaciones;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioEntidadesBeneficiarias;
import ar.utn.donatrack.donaciones.models.asignacion.RankingPrecalculado;
import ar.utn.donatrack.donaciones.models.asignacion.ResultadoMatchmaking;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

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
 * DonacionComandosService.asignar(), que es el único punto donde una donación cambia a
 * ASIGNACION_REALIZADA.
 *
 * POR LOTES (Entrega 5): antes leía todas las donaciones en depósito juntas, en
 * una sola transacción que duraba todo el proceso. Ahora las recorre de a
 * lotes (donaciones.procesos.tamanio-lote), cada uno en una transacción corta:
 * en memoria quedan solo las donaciones del lote y los rankings calculados, que
 * son chicos. Las entidades se cargan una vez por lote: el algoritmo de
 * sub-atendidos puntúa a todas, así que no se pueden prefiltrar por
 * subcategoría sin cambiar su resultado.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsignacionBatchService {

    private final RepositorioDonaciones donacionesRepositorio;
    private final AsignacionDonacionesService asignacionService;
    private final RepositorioCandidatosAsignacion candidatosRepositorio;
    private final RepositorioEntidadesBeneficiarias entidadesRepositorio;
    private final DonacionesProperties propiedades;
    private final TransactionOperations transaccion;

    /**
     * Corre todos los días a las 03:30 de Argentina, que es la franja de menor
     * uso del depósito. La zona se declara explícitamente: sin ella el horario
     * dependía de la zona del servidor (UTC en un contenedor).
     */
    @Scheduled(cron = "${donaciones.procesos.cron-matchmaking:0 30 3 * * *}", zone = FechaHoraArgentina.ZONA_ID)
    @SchedulerLock(name = "matchmaking-nocturno", lockAtMostFor = "PT1H")
    public void ejecutarMatchmakingNocturno() {
        LocalDate hoy = FechaHoraArgentina.hoy();
        int tamanio = propiedades.procesos().tamanioLote();
        log.info("[ASIGNACION] Inicio del matchmaking nocturno.");

        List<RankingPrecalculado> rankings = new ArrayList<>();
        int procesadas = 0;
        int conVencidos = 0;
        UUID ultima = null;
        Lote lote;
        do {
            UUID desde = ultima;
            lote = Objects.requireNonNull(transaccion.execute(estado -> puntuarLote(desde, tamanio, hoy)));
            rankings.addAll(lote.rankings());
            procesadas += lote.leidas() - lote.conVencidos();
            conVencidos += lote.conVencidos();
            ultima = lote.ultima();
        } while (lote.leidas() == tamanio);

        // Los rankings de la corrida anterior se descartan (las necesidades de las
        // entidades pudieron cambiar durante el día y una recomendación vieja es
        // peor que ninguna), pero recién al final y de una vez: si el proceso se
        // corta a mitad de camino, la bandeja anterior queda entera.
        transaccion.executeWithoutResult(estado -> candidatosRepositorio.reemplazarTodos(rankings));

        long conCoincidencias = rankings.stream().filter(RankingPrecalculado::huboCoincidencias).count();
        long sinCandidatas = rankings.stream().filter(RankingPrecalculado::sinCandidatas).count();
        log.info("[ASIGNACION] Matchmaking finalizado. Procesadas: {} | Con bienes vencidos (fuera): {} | "
                        + "Con coincidencia entre todos los algoritmos: {} | Sin ninguna candidata: {}",
                procesadas, conVencidos, conCoincidencias, sinCandidatas);
    }

    /**
     * Lee el lote siguiente a la donación indicada y calcula sus rankings. Una
     * donación con bienes vencidos no se le recomienda a nadie: queda fuera de la
     * bandeja hasta que la persona administradora la pase a VENCIDA
     * (GET /donaciones?vencidas=true las lista).
     */
    private Lote puntuarLote(UUID desde, int tamanio, LocalDate hoy) {
        List<Donacion> donaciones = donacionesRepositorio.obtenerLoteEnEstado(EN_DEPOSITO, desde, tamanio);
        if (donaciones.isEmpty()) {
            return new Lote(List.of(), 0, 0, desde);
        }
        List<Donacion> vencidas = donaciones.stream().filter(d -> d.tieneBienesVencidosAl(hoy)).toList();
        if (!vencidas.isEmpty()) {
            log.warn("[ASIGNACION] {} donaciones en depósito tienen bienes vencidos y quedan fuera del matchmaking: {}",
                    vencidas.size(), vencidas.stream().map(Donacion::getId).toList());
        }

        // Las entidades son las mismas para todas las donaciones del lote: se
        // cargan una vez y no una vez por donación (Entrega 5).
        List<EntidadBeneficiaria> entidades = entidadesRepositorio.buscarTodas();
        List<RankingPrecalculado> rankings = donaciones.stream()
                .filter(donacion -> !vencidas.contains(donacion))
                .map(donacion -> calcularRanking(donacion, entidades))
                .flatMap(Optional::stream)
                .toList();
        return new Lote(rankings, donaciones.size(), vencidas.size(), donaciones.getLast().getId());
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

    /** Lo que deja un lote: sus rankings, cuántas donaciones leyó y la última, que es donde sigue el próximo. */
    private record Lote(List<RankingPrecalculado> rankings, int leidas, int conVencidos, UUID ultima) {
    }
}
