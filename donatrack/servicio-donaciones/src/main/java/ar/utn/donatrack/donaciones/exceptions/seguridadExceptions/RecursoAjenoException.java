package ar.utn.donatrack.donaciones.exceptions.seguridadExceptions;

import ar.utn.donatrack.donaciones.exceptions.AccesoDenegadoException;

/**
 * Lo pedido pertenece a otra persona u organización: el perfil de otro donante,
 * una donación de otro, la entidad de otra organización. Responde 403.
 */
public class RecursoAjenoException extends AccesoDenegadoException {

    public RecursoAjenoException(String mensaje) {
        super(mensaje);
    }
}
