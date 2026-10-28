package ar.utn.donatrack.donaciones.models.donacion.estado;

import java.util.Map;

import static ar.utn.donatrack.donaciones.models.donacion.estado.EstadosDonacion.ENTREGADA;

public class EntregadaState extends EstadoDonacionBase {

    public EntregadaState() {
        super(ENTREGADA, Map.of());
    }
}
