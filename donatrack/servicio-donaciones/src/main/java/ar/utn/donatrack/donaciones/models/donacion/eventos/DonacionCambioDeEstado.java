package ar.utn.donatrack.donaciones.models.donacion.eventos;

import java.util.UUID;

import static ar.utn.donatrack.donaciones.models.donacion.estado.EstadosDonacion.EN_DEPOSITO;

/** La donación pasó de un estado a otro (cualquier transición, automática o manual). */
public record DonacionCambioDeEstado(UUID idDonacion, String estadoPrevio, String estadoNuevo) {

    /** Dejó el depósito: su ranking precalculado ya no sirve. */
    public boolean salioDelDeposito() {
        return EN_DEPOSITO.equals(estadoPrevio) && !EN_DEPOSITO.equals(estadoNuevo);
    }
}
