package ar.utn.donatrack.donaciones.model.donacion.bien;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
public class BienConEstado extends Bien {
    private boolean esNuevo;
}
