package ar.utn.donatrack.incentivos.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Setter
@Builder
public class RankingMensual {
    private LocalDateTime periodoInicio;
    private LocalDateTime fechaCalculo;
    private List<PosicionRanking> posiciones;

    public static RankingMensual calcular(List<Donante> donantes, LocalDateTime fechaCalculo) {
        YearMonth periodo = YearMonth.from(fechaCalculo);
        LocalDateTime periodoInicio = periodo.atDay(1).atStartOfDay();
        List<PosicionRanking> posiciones = calcularPosiciones(donantes, periodo, periodoInicio);

        return RankingMensual.builder()
                .periodoInicio(periodoInicio)
                .fechaCalculo(fechaCalculo)
                .posiciones(posiciones)
                .build();
    }

    private static List<PosicionRanking> calcularPosiciones(List<Donante> donantes, YearMonth periodo, LocalDateTime periodoInicio) {
        AtomicInteger puesto = new AtomicInteger(1);

        return donantes.stream()
                .sorted(criterioRanking(periodo))
                .map(donante -> crearPosicion(donante, puesto.getAndIncrement(), periodo, periodoInicio))
                .toList();
    }

    private static Comparator<Donante> criterioRanking(YearMonth periodo) {
        MetricasDonante metricas = new MetricasDonante();

        return Comparator.comparingInt((Donante donante) -> misionesCompletadas(donante, periodo))
                .reversed()
                .thenComparing(Comparator.comparingInt((Donante donante) -> metricas.donacionesMesActual(donante)).reversed())
                .thenComparing(Comparator.comparingInt((Donante donante) -> metricas.totalDonacionesHistoricas(donante)).reversed());
    }

    private static PosicionRanking crearPosicion(Donante donante, int puesto, YearMonth periodo, LocalDateTime periodoInicio) {
        return PosicionRanking.builder()
                .donante(donante)
                .puesto(puesto)
                .misionesCompletadas(misionesCompletadas(donante, periodo))
                .diaDeCreacion(periodoInicio)
                .build();
    }

    private static int misionesCompletadas(Donante donante, YearMonth periodo) {
        return donante.misionesCompletadasEnPeriodo(periodo.getMonthValue(), periodo.getYear());
    }

    public List<PosicionRanking> topTres() {
        return posiciones.stream()
                .sorted(Comparator.comparingInt(PosicionRanking::getPuesto))
                .limit(3)
                .toList();
    }

    public List<PosicionRanking> top3() {
        return topTres();
    }

    public int posicionDe(UUID donanteId) {
        return posiciones.stream()
                .filter(posicion -> donanteId.equals(posicion.getDonanteId()))
                .mapToInt(PosicionRanking::getPuesto)
                .findFirst()
                .orElse(0);
    }

    public int getMes() {
        return periodoInicio.getMonthValue();
    }

    public int getAnio() {
        return periodoInicio.getYear();
    }
}
