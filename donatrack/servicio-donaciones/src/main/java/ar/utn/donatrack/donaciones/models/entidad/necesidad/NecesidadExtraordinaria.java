package ar.utn.donatrack.donaciones.models.entidad.necesidad;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NecesidadExtraordinaria extends Necesidad {

    @Override
    public void recibirDonacion(int cantidad) {
        this.cantidadRecibida += cantidad;
    }

    @Override
    public boolean estaSatisfecha() {
        return this.cantidadRecibida >= this.cantidadObjetivo;
    }
}