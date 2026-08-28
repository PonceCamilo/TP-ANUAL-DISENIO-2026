package ar.utn.donatrack.donaciones.exceptions.entidadesExceptions;

import java.util.UUID;

public class NecesidadNoEncontradaException extends RuntimeException {
    public NecesidadNoEncontradaException(UUID necesidadId, UUID campaniaId) {
        super("La necesidad con ID " + necesidadId + " no existe o no pertenece a la campaña " + campaniaId);
    }
}
