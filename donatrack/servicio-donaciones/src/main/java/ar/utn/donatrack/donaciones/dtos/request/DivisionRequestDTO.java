package ar.utn.donatrack.donaciones.dtos.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.Map;
import java.util.UUID;

/**
 * Entrega 6: cuánto de cada bien pasa a la donación nueva, por id de bien. Los
 * bienes que no figuran quedan enteros en la donación original.
 */
public record DivisionRequestDTO(@NotEmpty Map<UUID, @Positive Integer> cantidades) {
}
