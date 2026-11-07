package ar.utn.donatrack.donaciones.models.donacion.estado;

import java.util.Map;

import static ar.utn.donatrack.donaciones.models.donacion.estado.EstadosDonacion.ASIGNACION_REALIZADA;
import static ar.utn.donatrack.donaciones.models.donacion.estado.EstadosDonacion.LISTA_PARA_ENTREGAR;

public class AsignacionRealizadaState extends EstadoDonacionBase {

    public AsignacionRealizadaState() {
        super(ASIGNACION_REALIZADA, Map.of(
                LISTA_PARA_ENTREGAR, ListaParaEntregarState::new));
    }
}
