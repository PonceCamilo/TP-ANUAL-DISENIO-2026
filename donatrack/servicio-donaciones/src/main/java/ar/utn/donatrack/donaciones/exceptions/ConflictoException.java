package ar.utn.donatrack.donaciones.exceptions;

/**
 * Categoría de error que GlobalExceptionHandler traduce a 409 Conflict.
 *
 * El pedido es válido pero choca con el estado actual de los datos: un email ya registrado, un borrado que dejaría donaciones huérfanas, una modificación concurrente.
 *
 * Cada excepción del dominio extiende la categoría que le corresponde, y es la
 * categoría la que define el código HTTP: sumar una excepción nueva no obliga a
 * tocar el handler.
 */
public abstract class ConflictoException extends RuntimeException {

    protected ConflictoException(String mensaje) {
        super(mensaje);
    }
}
