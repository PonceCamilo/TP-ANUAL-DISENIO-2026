package ar.utn.donatrack.brokerlogistica.dtos.donaciones;

import java.util.List;
import java.util.UUID;

/** Mismo contrato que InicioRutaCallbackDTO de servicio-donaciones. */
public record InicioRutaCallback(UUID idRuta, List<UUID> idsDonaciones, String urlMapaInteractivo) {
}
