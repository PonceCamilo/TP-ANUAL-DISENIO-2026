package ar.utn.donatrack.donaciones.models.donacion.estado;

import java.util.Map;

public class EntregaFallidaState extends EstadoDonacionBase {

    public EntregaFallidaState() {
        super("ENTREGA_FALLIDA", Map.of(
            "EN_DEPOSITO", EnDepositoState::new
        ));
    }

}
