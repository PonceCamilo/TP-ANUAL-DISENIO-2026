package ar.utn.donatrack.logistica.exceptions;

import ar.utn.donatrack.logistica.models.entrega.EstadoEntrega;

public class TransicionEntregaIlegalException extends RuntimeException {
    public TransicionEntregaIlegalException(EstadoEntrega estadoActual, EstadoEntrega estadoNuevo) {
        super("Transición inválida: " + estadoActual + " → " + estadoNuevo);
    }
}
