package ar.utn.donatrack.logistica.dtos.request;

import jakarta.validation.Valid;
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
 *
 * El token de correlación NO viaja en este body: el proveedor lo manda en el
 * header {@code Authorization} (ver PlanificacionController.callback), para
 * no exponer una credencial de correlación como un campo más del JSON.
 */
@Getter
@Setter
@NoArgsConstructor
public class CallbackRutaRequestDTO {
    @NotNull
    private UUID loteId;
    @NotEmpty
    @Valid
    private List<CallbackVehiculoRutaDTO> rutas;
}
