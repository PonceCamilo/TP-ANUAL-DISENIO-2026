package ar.utn.donatrack.donaciones.dtos.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Payload que envía el Servicio de Logística cuando un chofer inicia su ruta.
 * Contiene las donaciones involucradas para que Donaciones dispare las
 * notificaciones correspondientes a entidades y donantes.
 */
public record InicioRutaCallbackDTO(
        @NotNull UUID idRuta,
        @NotEmpty List<UUID> idsDonaciones,
        @NotNull String urlMapaInteractivo
) {}
