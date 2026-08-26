package ar.utn.donatrack.donaciones.models.donante.estado;

import java.util.Map;

public class InactivoState extends EstadoDonante {

    public InactivoState() {
        super("INACTIVO", Map.of(
            "ACTIVO", ActivoState::new
        ));
    }

}
