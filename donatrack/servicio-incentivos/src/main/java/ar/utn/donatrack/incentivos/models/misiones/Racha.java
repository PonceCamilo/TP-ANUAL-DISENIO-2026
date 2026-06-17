package ar.utn.donatrack.incentivos.models.misiones;

import ar.utn.donatrack.incentivos.models.Donante;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Racha extends Mision {
    private int mesesRequeridos;

    @Override
    public boolean estaCompletada(Donante donante) {
        return progresoActual(donante) >= mesesRequeridos;
    }

    @Override
    public int progresoActual(Donante donante) {
        return donante.getMesesConsecutivosDonando();
    }
}