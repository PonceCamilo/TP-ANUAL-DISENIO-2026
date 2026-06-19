package ar.utn.donatrack.incentivos.models.misiones;

import ar.utn.donatrack.incentivos.models.Donante;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HabilDonador extends Mision {
    private int cantidadBienesRequerida;

    @Override
    public boolean estaCompletada(Donante donante) {     
        return progresoActual(donante) >= cantidadBienesRequerida;
    }

    @Override
    public int progresoActual(Donante donante) {     
        return donante.getRecordBienesUnicaDonacion();
    }
}