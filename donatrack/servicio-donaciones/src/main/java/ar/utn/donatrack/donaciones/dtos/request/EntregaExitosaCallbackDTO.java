package ar.utn.donatrack.donaciones.dtos.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Payload que envía el Servicio de Logística cuando la entidad beneficiaria
 * confirma la recepción satisfactoria de una donación.
 *
 * fotos (Entrega 6, requisito 12) es opcional: las URLs de las fotos que cargó
 * la entidad al confirmar. Si el proveedor no las manda, llega vacía y el aviso
 * se procesa igual.
 */
public record EntregaExitosaCallbackDTO(
        @NotNull UUID idDonacion,
        @NotNull UUID idCamion,
        @NotNull String patenteCamion,
        @NotNull LocalDateTime fechaHoraEntrega,
        @Size(max = 10, message = "Se aceptan hasta 10 fotos por entrega")
        List<@Size(max = 2048) @Pattern(regexp = "https?://\\S+",
                message = "Cada foto tiene que ser una URL que empiece con http:// o https://") String> fotos
) {

    public EntregaExitosaCallbackDTO {
        fotos = fotos == null ? List.of() : List.copyOf(fotos);
    }

    /** Aviso sin fotos: el formato anterior a la Entrega 6, que el broker sigue pudiendo mandar. */
    public EntregaExitosaCallbackDTO(UUID idDonacion, UUID idCamion, String patenteCamion,
                                     LocalDateTime fechaHoraEntrega) {
        this(idDonacion, idCamion, patenteCamion, fechaHoraEntrega, List.of());
    }
}
