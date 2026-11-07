package ar.utn.donatrack.logistica.exceptions;

import java.util.UUID;

public class EntregaNoEncontradaException extends RuntimeException {
    public EntregaNoEncontradaException(UUID id) {
        super("No existe una entrega con id " + id);
    }

    public static EntregaNoEncontradaException porDonacion(UUID idDonacion) {
        return new EntregaNoEncontradaException("No existe una entrega para la donación " + idDonacion);
    }

    private EntregaNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}
