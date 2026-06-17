//TODO package y import

@Getter
@Setter
@Builder
public class RankingMensual { 
    private int mes;
    private int anio;
    private LocalDate fechaCalculo;
    private List<PosicionRanking> posiciones;

    public List<PosicionRanking> top3() {
        return posiciones.stream().sorted(Comparator.comparingInt(PosicionRanking::getPuesto))
            .limit(3).toList(); //agarra una lista, se queda con los primeros 3  y los devuelve en una lista nueva.
    }   
}