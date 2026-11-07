package ar.utn.donatrack.donaciones.exceptions;

/**
 * Categoría de error que GlobalExceptionHandler traduce a 400 Bad Request.
 *
 * El pedido está mal formado o trae datos que no cumplen las reglas de entrada: es un error de quien llama y no tiene sentido reintentarlo igual.
 *
 * Cada excepción del dominio extiende la categoría que le corresponde, y es la
 * categoría la que define el código HTTP: sumar una excepción nueva no obliga a
 * tocar el handler.
 */
public abstract class SolicitudInvalidaException extends RuntimeException {

    protected SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
