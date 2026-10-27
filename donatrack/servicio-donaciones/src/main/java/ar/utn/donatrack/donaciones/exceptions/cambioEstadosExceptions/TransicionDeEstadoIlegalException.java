package ar.utn.donatrack.donaciones.exceptions.cambioEstadosExceptions;

import ar.utn.donatrack.donaciones.exceptions.ReglaDeNegocioException;

/**
 * La máquina de estados no permite pasar del estado actual al pedido, ya sea de
 * una donación o de una persona donante. Antes había una excepción por cada
 * jerarquía, con mensajes distintos para la misma regla.
 */
public class TransicionDeEstadoIlegalException extends ReglaDeNegocioException {

    public TransicionDeEstadoIlegalException(String estadoActual, String estadoDestino) {
        super("Transición de estado no permitida: no es posible pasar de " + estadoActual + " a " + estadoDestino + ".");
    }
}
