package ar.utn.donatrack.incentivos.models.misiones;

import ar.utn.donatrack.incentivos.models.Donante;
import jakarta.persistence.Embeddable;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class ProgresoMision {
    @ManyToOne
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

    public void cambiarMisionActual(Mision mision) {
        this.misionActual = mision;
    }
}
