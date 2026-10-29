package ar.utn.donatrack.donaciones.models.donacion.estado;

import java.util.Map;

import static ar.utn.donatrack.donaciones.models.donacion.estado.EstadosDonacion.VENCIDA;

public class VencidaState extends EstadoDonacionBase {

    public VencidaState() {
        super(VENCIDA, Map.of());
    }
}
