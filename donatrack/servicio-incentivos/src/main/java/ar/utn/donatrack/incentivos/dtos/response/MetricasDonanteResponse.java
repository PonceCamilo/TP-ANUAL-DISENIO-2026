package ar.utn.donatrack.incentivos.dtos.response;

import ar.utn.donatrack.incentivos.models.Donante;
import ar.utn.donatrack.incentivos.models.MetricasDonante;

import java.util.UUID;

public record MetricasDonanteResponse(
        UUID donanteId,
        String categoriaActual,
        int totalDonacionesHistoricas,
        int donacionesMesActual,
        int organizacionesAyudadas,
        int posicionRanking
) {
    public static MetricasDonanteResponse desde(Donante donante, int posicionRanking) {
        MetricasDonante metricas = new MetricasDonante();
        return new MetricasDonanteResponse(
                donante.getId(),
                donante.getCategoria() != null ? donante.getCategoria().getClass().getSimpleName() : "Sin Categoria",
                metricas.totalDonacionesHistoricas(donante),
                metricas.donacionesMesActual(donante),
                metricas.organizacionesAyudadas(donante),
                posicionRanking
        );
    }
}
