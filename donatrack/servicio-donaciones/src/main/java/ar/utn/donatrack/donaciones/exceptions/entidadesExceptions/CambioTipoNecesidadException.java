package ar.utn.donatrack.donaciones.exceptions.entidadesExceptions;

/**
 * Una necesidad no puede cambiar de tipo (EXTRAORDINARIA ↔ RECURRENTE) al
 * actualizarse: son clases distintas y hacerlo implicaría reconstruir el objeto
 * perdiendo su historial. Para cambiar el tipo hay que eliminar la necesidad y
 * crear una nueva.
 */
public class CambioTipoNecesidadException extends RuntimeException {
    public CambioTipoNecesidadException(String tipoActual, String tipoRecibido) {
        super("No se puede cambiar el tipo de una necesidad: la necesidad es " + tipoActual
                + " y se recibió " + tipoRecibido + ". Elimine la necesidad y cree una nueva.");
    }
}
