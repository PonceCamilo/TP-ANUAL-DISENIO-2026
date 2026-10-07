package ar.utn.donatrack.donaciones.exceptions.logisticaExceptions;

/**
 * No se pudo despachar a logística: el broker respondió 503 (ningún proveedor
 * disponible) o no respondió en absoluto.
 *
 * Se traduce a 503 para que el cliente sepa que puede reintentar más tarde. Las
 * donaciones quedan en ASIGNACION_REALIZADA, sin tocar.
 */
public class LogisticaNoDisponibleException extends RuntimeException {

    public LogisticaNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
