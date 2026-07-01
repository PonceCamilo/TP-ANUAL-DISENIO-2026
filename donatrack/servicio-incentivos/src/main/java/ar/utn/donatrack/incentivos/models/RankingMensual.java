package ar.utn.donatrack.incentivos.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.Comparator;
import java.util.UUID;

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

    public int posicionDe(UUID donanteId) {
        return posiciones.stream()
                .filter(posicion -> donanteId.equals(posicion.getDonanteId()))
                .mapToInt(PosicionRanking::getPuesto)
                .findFirst()
                .orElse(0);
    }
}
