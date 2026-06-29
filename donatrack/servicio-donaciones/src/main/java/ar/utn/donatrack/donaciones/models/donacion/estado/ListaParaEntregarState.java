package ar.utn.donatrack.donaciones.models.donacion.estado;

import java.util.Map;

public class ListaParaEntregarState extends EstadoDonacionBase {

    public ListaParaEntregarState() {
        super("LISTA_PARA_ENTREGAR", Map.of(
            "EN_TRASLADO", EnTrasladoState::new
        ));
    }
}
