package ar.utn.donatrack.donaciones.model.entidad;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NecesidadExtraordinaria extends Necesidad {
    private int cantidadRequerida;
    private int cantidadRecibida;

    public void recibirDonacion(int cantidad){
        this.cantidadRequerida += cantidad;
    }

    @Override
    public boolean estaSatisfecha() {
        return this.cantidadRecibida >= this.cantidadRequerida;
    }
}