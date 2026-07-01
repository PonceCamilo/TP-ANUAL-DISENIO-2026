package ar.utn.donatrack.incentivos.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Getter
@Setter
public class PosicionRanking {
    private UUID donanteId;
    private int puesto;
    private int misionesCompletadas;
    private int donacionesMesActual;
    private int totalDonacionesHistoricas;
    private LocalDateTime primerDiaDelMes;
}
