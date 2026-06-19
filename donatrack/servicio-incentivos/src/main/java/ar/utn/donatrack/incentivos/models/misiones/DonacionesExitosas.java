package ar.utn.donatrack.incentivos.models.misiones;

import ar.utn.donatrack.incentivos.models.Donante;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DonacionesExitosas extends Mision {
    private int cantidadDonacionesRequerida;
    
    @Override
    public boolean estaCompletada(Donante donante) {    
        return progresoActual(donante) >= cantidadDonacionesRequerida;
    }

    @Override
    public int progresoActual(Donante donante) {
        return donante.getDonacionesExitosas();
    }
}