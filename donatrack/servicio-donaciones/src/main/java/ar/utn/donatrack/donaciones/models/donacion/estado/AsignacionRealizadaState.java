package ar.utn.donatrack.donaciones.models.donacion.estado;

import java.util.Map;

public class AsignacionRealizadaState extends EstadoDonacionBase {

    public AsignacionRealizadaState() {
        super("ASIGNACION_REALIZADA", Map.of(
            "LISTA_PARA_ENTREGAR", ListaParaEntregarState::new
        ));
    }

}
