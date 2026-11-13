package ar.utn.donatrack.donaciones.dtos.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * La administración devolvió la entrega al depósito. La donación vuelve a
 * EN_DEPOSITO para asignarla y despacharla de nuevo.
 */
public record RegresoDepositoCallbackDTO(@NotNull UUID idDonacion) {
}
