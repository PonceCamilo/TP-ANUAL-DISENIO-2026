package ar.utn.donatrack.donaciones.models.donante.estado;

import java.util.Map;

import static ar.utn.donatrack.donaciones.models.donante.estado.EstadosDonante.ACTIVO;
import static ar.utn.donatrack.donaciones.models.donante.estado.EstadosDonante.INACTIVO;

public class InactivoState extends EstadoDonante {

    public InactivoState() {
        super(INACTIVO, Map.of(ACTIVO, ActivoState::new));
    }
}
