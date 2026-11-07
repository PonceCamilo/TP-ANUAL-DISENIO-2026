package ar.utn.donatrack.donaciones.models.donacion.estado;

import ar.utn.donatrack.donaciones.exceptions.cambioEstadosExceptions.FaltaJustificacionException;

import java.util.Map;

import static ar.utn.donatrack.donaciones.models.donacion.estado.EstadosDonacion.ENTREGADA;
import static ar.utn.donatrack.donaciones.models.donacion.estado.EstadosDonacion.ENTREGA_FALLIDA;
import static ar.utn.donatrack.donaciones.models.donacion.estado.EstadosDonacion.EN_TRASLADO;

public class EnTrasladoState extends EstadoDonacionBase {

    public EnTrasladoState() {
        super(EN_TRASLADO, Map.of(
                ENTREGADA, EntregadaState::new,
                ENTREGA_FALLIDA, EntregaFallidaState::new));
    }

    @Override
    protected void validarTransicion(String estadoDestino, String justificacion) {
        if (ENTREGA_FALLIDA.equals(estadoDestino) && sinJustificacion(justificacion)) {
            throw new FaltaJustificacionException("Es necesaria una justificación para registrar una entrega fallida.");
        }
    }
}
