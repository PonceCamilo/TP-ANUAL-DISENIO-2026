package ar.utn.donatrack.donaciones.exceptions.entidadesExceptions;

import ar.utn.donatrack.donaciones.exceptions.NoEncontradoException;

import java.util.UUID;

public class NecesidadNoEncontradaException extends NoEncontradoException {
    public NecesidadNoEncontradaException(UUID necesidadId, UUID campaniaId) {
        super("La necesidad con ID " + necesidadId + " no existe o no pertenece a la campaña " + campaniaId);
    }
}
