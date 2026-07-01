package ar.utn.donatrack.logistica.validations;

import ar.utn.donatrack.logistica.exceptions.TransicionEntregaIlegalException;
import ar.utn.donatrack.logistica.models.entrega.EstadoEntrega;
import org.springframework.stereotype.Component;

/**
 * Reglas de transición de EstadoEntrega (mismo enfoque que DonacionesValidator
 * en servicio-donaciones: un enum simple + validación explícita, en vez de
 * una jerarquía de clases State completa).
 *
 * PENDIENTE -> EN_TRASLADO              (inicio de ruta)
 * EN_TRASLADO -> ENTREGADA | NO_RECIBIDA
 * NO_RECIBIDA -> PENDIENTE              (regreso a depósito)
 * ENTREGADA es terminal.
 */
@Component
public class EntregaValidator {

    public void validarTransicion(EstadoEntrega estadoActual, EstadoEntrega estadoNuevo) {
        boolean valida = switch (estadoActual) {
            case PENDIENTE -> estadoNuevo == EstadoEntrega.EN_TRASLADO;
            case EN_TRASLADO -> estadoNuevo == EstadoEntrega.ENTREGADA || estadoNuevo == EstadoEntrega.NO_RECIBIDA;
            case NO_RECIBIDA -> estadoNuevo == EstadoEntrega.PENDIENTE;
            case ENTREGADA -> false;
        };

        if (!valida) {
            throw new TransicionEntregaIlegalException(estadoActual, estadoNuevo);
        }
    }
}
