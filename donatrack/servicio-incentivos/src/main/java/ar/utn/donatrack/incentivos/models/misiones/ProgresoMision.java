package ar.utn.donatrack.incentivos.models.misiones;

import ar.utn.donatrack.incentivos.models.Donante;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProgresoMision {
    private Mision misionActual;

    public boolean completadaPor(Donante donante) {
        return misionActual != null && misionActual.estaCompletada(donante);
    }

    public int progresoActual(Donante donante) {
        return misionActual == null ? 0 : misionActual.progresoActual(donante);
    }

    public int distanciaRestante(Donante donante) {
        return misionActual == null ? 0 : misionActual.restante(donante);
    }
}
