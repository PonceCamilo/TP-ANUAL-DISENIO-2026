package ar.utn.donatrack.donaciones.exceptions.personasExceptions;

import ar.utn.donatrack.donaciones.exceptions.SolicitudInvalidaException;

/** El tipo de documento no existe o el número no corresponde a ese tipo (400). */
public class DocumentoInvalidoException extends SolicitudInvalidaException {
    public DocumentoInvalidoException(String detalle) {
        super("Documento inválido: " + detalle);
    }
}
