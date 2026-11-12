package ar.utn.donatrack.donaciones.exceptions;

/**
 * Categoría de error que GlobalExceptionHandler traduce a 403 Forbidden.
 *
 * Quien llama se identificó (trae un token válido), pero lo que pide no le
 * corresponde: los datos de otra persona donante, una donación que no es suya, la
 * entidad de otra organización (Entrega 6).
 *
 * Cada excepción del dominio extiende la categoría que le corresponde, y es la
 * categoría la que define el código HTTP: sumar una excepción nueva no obliga a
 * tocar el handler.
 */
public abstract class AccesoDenegadoException extends RuntimeException {

    protected AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
