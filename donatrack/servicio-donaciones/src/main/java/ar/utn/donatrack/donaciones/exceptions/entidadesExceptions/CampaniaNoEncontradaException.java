package ar.utn.donatrack.donaciones.exceptions.entidadesExceptions;

import java.util.UUID;

public class CampaniaNoEncontradaException extends RuntimeException {
    public CampaniaNoEncontradaException(UUID campaniaId, UUID entidadId) {
        super("La campaña con ID " + campaniaId + " no existe o no pertenece a la entidad " + entidadId);
    }
}