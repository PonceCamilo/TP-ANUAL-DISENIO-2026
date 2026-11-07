package ar.utn.donatrack.donaciones.exceptions;

/**
 * Categoría de error que GlobalExceptionHandler traduce a 422 Unprocessable Entity.
 *
 * El pedido se entiende pero viola una regla del dominio, por ejemplo una transición de estado que la máquina no permite.
 *
 * Cada excepción del dominio extiende la categoría que le corresponde, y es la
 * categoría la que define el código HTTP: sumar una excepción nueva no obliga a
 * tocar el handler.
 */
public abstract class ReglaDeNegocioException extends RuntimeException {

    protected ReglaDeNegocioException(String mensaje) {
        super(mensaje);
    }
}
