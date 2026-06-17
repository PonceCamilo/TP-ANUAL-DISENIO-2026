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
    public static MetricasDonanteResponse desde(Donante p) {
        return new MetricasDonanteResponse(
                p.getId(),
                p.getCategoria() != null ? p.getCategoria().getClass().getSimpleName() : "Sin Categoria", // <-- Corrección
                p.getTotalDonacionesHistoricas(),
                p.getDonacionesMesActual(),
                p.getOrganizacionesAyudadas(),
                p.getPosicionRanking()
        );
    }
}