package ar.utn.donatrack.logistica.exceptions;

/**
 * Se lanza cuando se pide planificar sin indicar camiones y no hay ninguno
 * DISPONIBLE. Se responde 503: logística no puede atender el pedido ahora,
 * y el broker de Donaciones puede derivarlo a otro servicio de logística.
 */
public class SinCamionesDisponiblesException extends RuntimeException {
    public SinCamionesDisponiblesException() {
        super("No hay camiones disponibles para planificar");
    }
}
