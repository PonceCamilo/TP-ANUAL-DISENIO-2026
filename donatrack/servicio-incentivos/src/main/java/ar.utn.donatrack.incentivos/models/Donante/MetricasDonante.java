package ar.utn.donatrack.incentivos.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID

@Getter
@Setter
@Builder

public class MetricasDonante {
    private UUID donanteId;
    private int totalDonacionesHistoricas;
    private int organizacionesAyudadas;
    private List<EvolucionPeriodo> evolucionPorPeriodo; 
}
