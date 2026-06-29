package ar.utn.donatrack.donaciones.models.donacion.estado;

import java.util.Map;

public class EnDepositoState extends EstadoDonacionBase {

    public EnDepositoState() {
        super("EN_DEPOSITO", Map.of(
            "ASIGNACION_REALIZADA", AsignacionRealizadaState::new,
            "VENCIDA",             VencidaState::new
        ));
    }

}
