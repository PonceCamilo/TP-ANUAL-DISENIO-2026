package ar.utn.donatrack.donaciones.exceptions.entidadesExceptions;

import ar.utn.donatrack.donaciones.exceptions.NoEncontradoException;

import java.util.UUID;

public class EntidadBeneficiariaNoEncontradaException extends NoEncontradoException {
    public EntidadBeneficiariaNoEncontradaException(UUID id) {
        super("No se encontró la Entidad Beneficiaria con el ID: " + id);
    }
}
