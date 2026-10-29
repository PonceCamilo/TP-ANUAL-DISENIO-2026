package ar.utn.donatrack.donaciones.exceptions;

/**
 * Categoría de error que GlobalExceptionHandler traduce a 404 Not Found.
 *
 * El recurso pedido no existe: una donación, una persona donante, una entidad, una campaña o una necesidad.
 *
 * Cada excepción del dominio extiende la categoría que le corresponde, y es la
 * categoría la que define el código HTTP: sumar una excepción nueva no obliga a
 * tocar el handler.
 */
public abstract class NoEncontradoException extends RuntimeException {

    protected NoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
