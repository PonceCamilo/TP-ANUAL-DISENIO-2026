package ar.utn.donatrack.incentivos.models.misiones;

import ar.utn.donatrack.incentivos.models.Donante;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Completitud extends Mision {
    private int categoriasRequeridas;

    @Override
    public boolean estaCompletada(Donante donante) {
        return progresoActual(donante) >= categoriasRequeridas;
    }

    @Override
    public int progresoActual(Donante donante) {
        return donante.getCategoriasDistintasDonadas();
    }
}