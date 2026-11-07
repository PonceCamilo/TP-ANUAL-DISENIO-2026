package ar.utn.donatrack.logistica.controllers;

import ar.utn.donatrack.logistica.dtos.request.CamionRequestDTO;
import ar.utn.donatrack.logistica.dtos.request.UbicacionRequestDTO;
import ar.utn.donatrack.logistica.dtos.response.CamionResponseDTO;
import ar.utn.donatrack.logistica.dtos.response.RutaResponseDTO;
import ar.utn.donatrack.logistica.interfaces.services.CamionServiceInterface;
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

@RestController
@RequestMapping("/api/logistica/camiones")
@RequiredArgsConstructor
@Tag(name = "Camiones", description = "Gestión de la flota de camiones")
public class CamionController {

    private final CamionServiceInterface camionService;
    private final PlanificacionServiceInterface planificacionService;

    @Operation(
            summary = "Registrar camión",
            description = "Da de alta un camión en la flota con su capacidad y dimensiones.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Camión registrado",
                            content = @Content(schema = @Schema(implementation = CamionResponseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Request inválido")
            }
    )
    @PostMapping
    public ResponseEntity<CamionResponseDTO> registrar(@Valid @RequestBody CamionRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(camionService.registrar(dto));
    }

    @Operation(
            summary = "Listar camiones",
            description = "Devuelve todos los camiones registrados en la flota.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Flota de camiones",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = CamionResponseDTO.class))))
            }
    )
    @GetMapping
    public ResponseEntity<List<CamionResponseDTO>> obtenerTodos() {
        return ResponseEntity.ok(camionService.obtenerTodos());
    }

    @Operation(
            summary = "Obtener camión por ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Camión encontrado",
                            content = @Content(schema = @Schema(implementation = CamionResponseDTO.class))),
                    @ApiResponse(responseCode = "404", description = "Camión no encontrado")
            }
    )
    @GetMapping("/{id}")
    public ResponseEntity<CamionResponseDTO> obtenerPorId(
            @Parameter(description = "ID del camión", example = "88888888-8888-8888-8888-888888888888")
            @PathVariable UUID id) {
        return ResponseEntity.ok(camionService.obtenerPorId(id));
    }

    @Operation(
            summary = "Pasar camión a mantenimiento",
            description = "Solo si está DISPONIBLE y no tiene una ruta PLANIFICADA o INICIADA. "
                    + "Un camión EN_RUTA o con reparto ya armado responde 409.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Camión en mantenimiento",
                            content = @Content(schema = @Schema(implementation = CamionResponseDTO.class))),
                    @ApiResponse(responseCode = "404", description = "Camión no encontrado"),
                    @ApiResponse(responseCode = "409", description = "No se puede sacar de servicio")
            }
    )
    @PostMapping("/{id}/mantenimiento")
    public ResponseEntity<CamionResponseDTO> pasarAMantenimiento(
            @Parameter(description = "ID del camión") @PathVariable UUID id) {
        return ResponseEntity.ok(camionService.pasarAMantenimiento(id));
    }

    @Operation(
            summary = "Volver a dejar el camión disponible",
            description = "Solo desde MANTENIMIENTO, y si no quedó una ruta abierta.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Camión disponible",
                            content = @Content(schema = @Schema(implementation = CamionResponseDTO.class))),
                    @ApiResponse(responseCode = "404", description = "Camión no encontrado"),
                    @ApiResponse(responseCode = "409", description = "El estado actual no permite volver a disponible")
            }
    )
    @PostMapping("/{id}/disponible")
    public ResponseEntity<CamionResponseDTO> volverADisponible(
            @Parameter(description = "ID del camión") @PathVariable UUID id) {
        return ResponseEntity.ok(camionService.volverADisponible(id));
    }

    @Operation(
            summary = "Registrar la última ubicación del camión",
            description = "La informa el chofer mientras el camión está EN_RUTA. "
                    + "Reemplaza el punto anterior: no se guarda el recorrido. "
                    + "La hora de la última actualización la pone el servidor.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Ubicación registrada",
                            content = @Content(schema = @Schema(implementation = CamionResponseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Latitud o longitud inválida"),
                    @ApiResponse(responseCode = "404", description = "Camión no encontrado"),
                    @ApiResponse(responseCode = "409", description = "El camión no está en ruta")
            }
    )
    @PostMapping("/{id}/ubicacion")
    public ResponseEntity<CamionResponseDTO> registrarUbicacion(
            @Parameter(description = "ID del camión") @PathVariable UUID id,
            @Valid @RequestBody UbicacionRequestDTO dto) {
        return ResponseEntity.ok(camionService.registrarUbicacion(id, dto));
    }

    @Operation(
            summary = "Obtener ruta vigente del camión",
            description = "Ruta vigente del camión, consumida por la app del chofer.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Ruta vigente del camión",
                            content = @Content(schema = @Schema(implementation = RutaResponseDTO.class))),
                    @ApiResponse(responseCode = "404", description = "El camión no tiene ruta vigente")
            }
    )
    @GetMapping("/{id}/ruta")
    public ResponseEntity<RutaResponseDTO> obtenerRutaVigente(
            @Parameter(description = "ID del camión", example = "66666666-6666-6666-6666-666666666666")
            @PathVariable UUID id) {
        return ResponseEntity.ok(planificacionService.obtenerRutaVigentePorCamion(id));
    }
}
