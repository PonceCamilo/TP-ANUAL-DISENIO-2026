package ar.utn.donatrack.donaciones.exceptions.entidadesExceptions;

import java.util.UUID;

/**
 * No se puede borrar una entidad beneficiaria que tiene donaciones asignadas: la
 * donación la referencia por id y quedaría apuntando a nadie. Se responde 409.
 */
public class EntidadConDonacionesException extends RuntimeException {

    public EntidadConDonacionesException(UUID id) {
        super("La entidad beneficiaria " + id + " tiene donaciones asignadas y no se puede eliminar.");
    }
}
