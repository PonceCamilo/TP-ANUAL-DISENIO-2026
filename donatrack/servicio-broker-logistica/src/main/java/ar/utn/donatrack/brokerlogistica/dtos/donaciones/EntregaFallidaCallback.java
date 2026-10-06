package ar.utn.donatrack.brokerlogistica.dtos.donaciones;

import java.util.UUID;

/** Mismo contrato que EntregaFallidaCallbackDTO de servicio-donaciones. */
public record EntregaFallidaCallback(UUID idDonacion, String motivoFallo, boolean replanificable) {
}
