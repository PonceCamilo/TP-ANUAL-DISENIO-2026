package ar.utn.donatrack.donaciones.models.donacion.estado;

import java.util.Map;

import static ar.utn.donatrack.donaciones.models.donacion.estado.EstadosDonacion.ASIGNACION_REALIZADA;
import static ar.utn.donatrack.donaciones.models.donacion.estado.EstadosDonacion.EN_DEPOSITO;
import static ar.utn.donatrack.donaciones.models.donacion.estado.EstadosDonacion.VENCIDA;

public class EnDepositoState extends EstadoDonacionBase {

    public EnDepositoState() {
        super(EN_DEPOSITO, Map.of(
                ASIGNACION_REALIZADA, AsignacionRealizadaState::new,
                VENCIDA, VencidaState::new));
    }
}
