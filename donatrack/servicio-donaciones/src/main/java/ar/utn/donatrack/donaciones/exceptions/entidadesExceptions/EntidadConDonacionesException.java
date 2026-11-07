package ar.utn.donatrack.donaciones.exceptions.entidadesExceptions;

import ar.utn.donatrack.donaciones.exceptions.ConflictoException;

import java.util.UUID;

/**
 * No se puede borrar una entidad beneficiaria que tiene donaciones asignadas: la
 * donación la referencia por id y quedaría apuntando a nadie. Se responde 409.
 */
public class EntidadConDonacionesException extends ConflictoException {

    public EntidadConDonacionesException(UUID id) {
        super("La entidad beneficiaria " + id + " tiene donaciones asignadas y no se puede eliminar.");
    }
}
