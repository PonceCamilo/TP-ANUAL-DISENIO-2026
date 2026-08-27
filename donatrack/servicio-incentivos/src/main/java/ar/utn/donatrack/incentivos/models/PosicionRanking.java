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
    private Donante donante;
    private int puesto;
    private int misionesCompletadas;
    private LocalDateTime diaDeCreacion;

    public UUID getDonanteId() {
        if(donante == null) {
            return null;     // aca podria haber algo mejor
        }
        return donante.getId();
    }
}
