package ar.utn.donatrack.donaciones.exceptions;

/**
 * Categoría de error que GlobalExceptionHandler traduce a 503 Service Unavailable.
 *
 * Un servicio externo del que depende la operación no respondió o no tenía capacidad: quien llama puede reintentar más tarde.
 *
 * Cada excepción del dominio extiende la categoría que le corresponde, y es la
 * categoría la que define el código HTTP: sumar una excepción nueva no obliga a
 * tocar el handler.
 */
public abstract class ServicioNoDisponibleException extends RuntimeException {

    protected ServicioNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
