package ar.utn.donatrack.donaciones.models.donante.estado;

import java.util.Map;

public class BloqueadoState extends EstadoDonante {

    public BloqueadoState() {
        super("BLOQUEADO", Map.of(
            "ACTIVO", ActivoState::new
        ));
    }

}
