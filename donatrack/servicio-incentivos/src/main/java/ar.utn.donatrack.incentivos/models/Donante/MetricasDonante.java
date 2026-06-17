//TODO package y import

@Getter
@Setter
@Builder

public class MetricasDonante {
    private UUID donanteId;
    private int totalDonacionesHistoricas;
    private int organizacionesAyudadas;
    private List<EvolucionPeriodo> evolucionPorPeriodo; 
}
