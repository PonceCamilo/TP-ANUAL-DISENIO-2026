package ar.utn.donatrack.donaciones.exceptions.entidadesExceptions;

import java.util.UUID;

public class EntidadBeneficiariaNoEncontradaException extends RuntimeException {
    public EntidadBeneficiariaNoEncontradaException(UUID id) {
        super("No se encontró la Entidad Beneficiaria con el ID: " + id);
    }
}