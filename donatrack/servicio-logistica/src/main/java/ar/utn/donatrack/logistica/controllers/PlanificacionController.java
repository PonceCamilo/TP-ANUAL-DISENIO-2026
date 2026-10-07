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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Recibe donaciones en estado "Asignación Realizada" + camiones disponibles,
 * planifica sus rutas contra el proveedor externo de ruteo (una llamada
 * síncrona por camión, ver ProveedorRuteoExternoAdapter) y expone además
 * un callback alternativo para un proveedor externo que responda de forma
 * asíncrona.
 */
@RestController
@RequestMapping("/api/logistica/planificaciones")
@RequiredArgsConstructor
@Tag(name = "Planificaciones", description = "Particionado en lotes y planificación de rutas con el proveedor externo")
public class PlanificacionController {

    private final PlanificacionServiceInterface planificacionService;

    @Operation(
            summary = "Planificar rutas",
            description = "Recibe donaciones disponibles y, opcionalmente, los camiones a usar "
                    + "(si no se envían camionesIds se usan todos los camiones DISPONIBLE). Particiona en lotes de <=100 donaciones y, "
                    + "para cada uno, le pide al proveedor externo de ruteo (una llamada síncrona por camión) que planifique su ruta. "
                    + "Devuelve, para cada lote, la lista de camiones con los destinos y las entregas a realizar en cada uno; "
                    + "las entregas creadas quedan en estado LISTO_PARA_ENTREGAR.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lotes planificados, con sus rutas ya armadas",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = LoteResponseDTO.class)))),
                    @ApiResponse(responseCode = "400", description = "Request inválido"),
                    @ApiResponse(responseCode = "404", description = "Algún camión indicado no existe"),
                    @ApiResponse(responseCode = "503", description = "El proveedor externo de ruteo no respondió o no hay camiones disponibles")
            }
    )
    @PostMapping
    public ResponseEntity<List<LoteResponseDTO>> planificar(@Valid @RequestBody PlanificacionRequestDTO dto) {
        return ResponseEntity.ok(planificacionService.planificar(dto));
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
            description = "Endpoint alternativo para un proveedor externo que planifique de forma asíncrona y devuelva "
                    + "las rutas calculadas de un lote más adelante. El token de correlación viaja en el header "
                    + "Authorization (no en el body) y se valida contra el token generado al enviar el lote.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Callback procesado; rutas y entregas creadas"),
                    @ApiResponse(responseCode = "409", description = "Token de correlación inválido")
            }
    )
    @PostMapping("/callback")
    public ResponseEntity<Void> callback(
            @Parameter(description = "Token de correlación del lote, ej. \"Bearer <token>\" o el token a secas")
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody CallbackRutaRequestDTO dto) {
        planificacionService.registrarCallback(dto, tokenDesdeHeader(authorization));
        return ResponseEntity.ok().build();
    }

    private String tokenDesdeHeader(String authorization) {
        return authorization.regionMatches(true, 0, "Bearer ", 0, 7)
                ? authorization.substring(7)
                : authorization;
    }
}
