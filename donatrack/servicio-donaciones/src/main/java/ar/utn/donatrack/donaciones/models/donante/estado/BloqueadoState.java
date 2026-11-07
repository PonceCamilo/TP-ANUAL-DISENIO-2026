package ar.utn.donatrack.donaciones.models.donante.estado;

import java.util.Map;

import static ar.utn.donatrack.donaciones.models.donante.estado.EstadosDonante.ACTIVO;
import static ar.utn.donatrack.donaciones.models.donante.estado.EstadosDonante.BLOQUEADO;

public class BloqueadoState extends EstadoDonante {

    public BloqueadoState() {
        super(BLOQUEADO, Map.of(ACTIVO, ActivoState::new));
    }
}
