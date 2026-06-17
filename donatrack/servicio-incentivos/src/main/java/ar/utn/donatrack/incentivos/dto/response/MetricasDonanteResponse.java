package ar.utn.donatrack.incentivos.dto.response;

import ar.utn.donatrack.incentivos.model.PerfilDonante;

import java.util.UUID;

public record MetricasDonanteResponse(
        UUID donanteId,
        String categoriaActual,
        int totalDonacionesHistoricas,
        int donacionesMesActual,
        int organizacionesAyudadas,
        int posicionRanking
) {
    public static MetricasDonanteResponse desde(PerfilDonante p) {
        return new MetricasDonanteResponse(
                p.getDonanteId(),
                p.getCategoria().name(),
                p.getTotalDonacionesHistoricas(),
                p.getDonacionesMesActual(),
                p.getOrganizacionesAyudadas(),
                p.getPosicionRanking()
        );
    }
}
