package ar.utn.donatrack.logistica.exceptions;

import java.util.UUID;

public class RutaNoEncontradaException extends RuntimeException {
    public RutaNoEncontradaException(UUID id) {
        super("No existe una ruta con id " + id);
    }

    private RutaNoEncontradaException(String mensaje) {
        super(mensaje);
    }

    public static RutaNoEncontradaException paraEntrega(UUID entregaId) {
        return new RutaNoEncontradaException("No existe una ruta que contenga la entrega " + entregaId);
    }

    public static RutaNoEncontradaException paraCamion(UUID camionId) {
        return new RutaNoEncontradaException("El camión " + camionId + " no tiene una ruta vigente");
    }
}
