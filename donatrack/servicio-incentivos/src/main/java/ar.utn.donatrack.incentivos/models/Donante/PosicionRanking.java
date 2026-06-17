package ar.utn.donatrack.incentivos.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class PosicionRanking {
    private UUID donanteId;
    private int puesto;
    private int misionesCompletadas;
}