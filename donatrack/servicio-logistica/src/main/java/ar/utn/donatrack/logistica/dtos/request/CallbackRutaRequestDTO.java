package ar.utn.donatrack.logistica.dtos.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * Payload que el proveedor externo de ruteo envía a
 * POST /api/logistica/planificaciones/callback una vez que terminó de
 * calcular las rutas de un lote.
 */
@Getter
@Setter
@NoArgsConstructor
public class CallbackRutaRequestDTO {
    @NotNull
    private UUID loteId;
    @NotBlank
    private String tokenCorrelacion;
    @NotEmpty
    @Valid
    private List<CallbackVehiculoRutaDTO> rutas;
}
