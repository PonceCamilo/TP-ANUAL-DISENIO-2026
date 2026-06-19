package ar.utn.donatrack.donaciones.models.entidad.necesidad;

import lombok.Getter;
import lombok.Setter;

/**
 * Necesidad que surge ante situaciones excepcionales (inundación, mudanza, etc.).
 * Se satisface cuando la cantidad recibida iguala o supera el objetivo, sin vencimiento.
 */

@Getter
@Setter
public class NecesidadExtraordinaria extends Necesidad {

    @Override
    public boolean estaSatisfecha() {
        return this.cantidadRecibida >= this.cantidadObjetivo;
    }
}
