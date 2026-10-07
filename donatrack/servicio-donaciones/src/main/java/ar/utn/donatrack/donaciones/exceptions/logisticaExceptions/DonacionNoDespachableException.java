package ar.utn.donatrack.donaciones.exceptions.logisticaExceptions;

/**
 * La donación no está en condiciones de despacharse a logística: no está en
 * ASIGNACION_REALIZADA, o su entidad beneficiaria no tiene dirección completa.
 *
 * Se traduce a 400: el pedido es inválido y reintentarlo igual no va a funcionar.
 */
public class DonacionNoDespachableException extends RuntimeException {

    public DonacionNoDespachableException(String mensaje) {
        super(mensaje);
    }
}
