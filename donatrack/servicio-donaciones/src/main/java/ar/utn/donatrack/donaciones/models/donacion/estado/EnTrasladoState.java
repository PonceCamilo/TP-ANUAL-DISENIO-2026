package ar.utn.donatrack.donaciones.models.donacion.estado;

import ar.utn.donatrack.donaciones.exceptions.cambioEstadosExceptions.FaltaJustificacionDonacionException;
import java.util.Map;

public class EnTrasladoState extends EstadoDonacionBase {

    public EnTrasladoState() {
        super("EN_TRASLADO", Map.of(
            "ENTREGADA",       EntregadaState::new,
            "ENTREGA_FALLIDA", EntregaFallidaState::new
        ));
    }

    public EstadoDonacionBase transicionarA(String estadoDestino, String justificacion) {
        if ("ENTREGA_FALLIDA".equals(estadoDestino) && (justificacion == null || justificacion.isBlank())) {
            throw new FaltaJustificacionDonacionException();
        }
        return super.transicionarA(estadoDestino, justificacion);
    }

}
