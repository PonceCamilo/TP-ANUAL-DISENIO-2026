package ar.utn.donatrack.donaciones.models.donacion.estado;

import java.util.Map;

import static ar.utn.donatrack.donaciones.models.donacion.estado.EstadosDonacion.EN_TRASLADO;
import static ar.utn.donatrack.donaciones.models.donacion.estado.EstadosDonacion.LISTA_PARA_ENTREGAR;

public class ListaParaEntregarState extends EstadoDonacionBase {

    public ListaParaEntregarState() {
        super(LISTA_PARA_ENTREGAR, Map.of(
                EN_TRASLADO, EnTrasladoState::new));
    }
}
