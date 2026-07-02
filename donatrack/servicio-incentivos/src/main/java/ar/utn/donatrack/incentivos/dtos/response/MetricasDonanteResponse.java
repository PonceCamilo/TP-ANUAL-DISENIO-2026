package ar.utn.donatrack.incentivos.dtos.response;

import ar.utn.donatrack.incentivos.models.Donante;

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
        return new MetricasDonanteResponse(
                donante.getId(),
                donante.getCategoria() != null ? donante.getCategoria().getClass().getSimpleName() : "Sin Categoria",
                donante.getMetricas().totalDonacionesHistoricas(),
                donante.getMetricas().donacionesMesActual(),
                donante.getMetricas().organizacionesAyudadas(),
                posicionRanking
        );
    }
}
