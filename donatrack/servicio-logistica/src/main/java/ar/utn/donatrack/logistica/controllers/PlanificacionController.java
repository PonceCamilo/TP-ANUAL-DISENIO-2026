package ar.utn.donatrack.logistica.controllers;

import ar.utn.donatrack.logistica.dtos.request.CallbackRutaRequestDTO;
import ar.utn.donatrack.logistica.dtos.request.PlanificacionRequestDTO;
import ar.utn.donatrack.logistica.dtos.response.LoteResponseDTO;
import ar.utn.donatrack.logistica.interfaces.services.PlanificacionServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Recibe donaciones en estado "Asignación Realizada" + camiones disponibles,
 * y expone el callback que usa el proveedor externo de ruteo para devolver
 * el resultado de la planificación (ver ProveedorRuteoExternoAdapter).
 */
@RestController
@RequestMapping("/api/logistica/planificaciones")
@RequiredArgsConstructor
@Tag(name = "Planificaciones", description = "Particionado en lotes y planificación de rutas con el proveedor externo")
public class PlanificacionController {

    private final PlanificacionServiceInterface planificacionService;

    @Operation(
            summary = "Planificar rutas",
            description = "Recibe donaciones y camiones, particiona en lotes de <=100 donaciones y los envía al proveedor externo de ruteo.",
            responses = {
                    @ApiResponse(responseCode = "202", description = "Lotes creados y enviados a planificar",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = LoteResponseDTO.class)))),
                    @ApiResponse(responseCode = "400", description = "Request inválido")
            }
    )
    @PostMapping
    public ResponseEntity<List<LoteResponseDTO>> planificar(@Valid @RequestBody PlanificacionRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(planificacionService.planificar(dto));
    }

    @Operation(
            summary = "Obtener lote de planificación",
            description = "Devuelve el estado de un lote de planificación por su ID.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lote encontrado",
                            content = @Content(schema = @Schema(implementation = LoteResponseDTO.class))),
                    @ApiResponse(responseCode = "404", description = "Lote no encontrado")
            }
    )
    @GetMapping("/{loteId}")
    public ResponseEntity<LoteResponseDTO> obtenerLote(
            @Parameter(description = "ID del lote de planificación")
            @PathVariable UUID loteId) {
        return ResponseEntity.ok(planificacionService.obtenerLote(loteId));
    }

    @Operation(
            summary = "Callback del proveedor externo de ruteo",
            description = "Endpoint que invoca el proveedor externo para devolver las rutas calculadas de un lote. Valida el token de correlación.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Callback procesado; rutas y entregas creadas"),
                    @ApiResponse(responseCode = "409", description = "Token de correlación inválido")
            }
    )
    @PostMapping("/callback")
    public ResponseEntity<Void> callback(@Valid @RequestBody CallbackRutaRequestDTO dto) {
        planificacionService.registrarCallback(dto);
        return ResponseEntity.ok().build();
    }
}
