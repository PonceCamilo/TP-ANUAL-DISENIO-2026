package ar.utn.donatrack.donaciones.exceptions;

/**
 * Se lanza cuando se intenta agregar un representante a una PersonaDonante
 * que no es PersonaJuridica.
 *
 * Reemplaza la clase IllegalArgumentException custom que ocultaba
 * la java.lang.IllegalArgumentException del JDK.
 */

public class RepresentanteEnPersonaNoJuridicaException extends RuntimeException {

    public RepresentanteEnPersonaNoJuridicaException() {
        super("Solo se pueden agregar representantes a personas jurídicas.");
    }
}
