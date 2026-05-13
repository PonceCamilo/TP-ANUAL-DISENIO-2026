package ar.utn.donatrack.donaciones.model.entidad;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NecesidadRecurrente extends Necesidad {
    private int cantidadObjetivoPorPeriodo;
    private int periodoEnDias;
    private int cantidadRecibidaEnPeriodo;

    @Override
    public boolean estaSatisfecha() {
        return this.cantidadRecibidaEnPeriodo >= this.cantidadObjetivoPorPeriodo;
    }
}