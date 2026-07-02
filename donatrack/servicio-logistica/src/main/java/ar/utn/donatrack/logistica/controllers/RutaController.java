package ar.utn.donatrack.logistica.controllers;

import ar.utn.donatrack.logistica.dtos.response.RutaResponseDTO;
import ar.utn.donatrack.logistica.interfaces.services.PlanificacionServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/logistica/rutas")
@RequiredArgsConstructor
@Tag(name = "Rutas", description = "Consulta e inicio de rutas de reparto")
public class RutaController {

    private final PlanificacionServiceInterface planificacionService;

    @Operation(
            summary = "Obtener ruta por ID",
            description = "Devuelve una ruta con sus paradas y entregas asociadas.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Ruta encontrada",
                            content = @Content(schema = @Schema(implementation = RutaResponseDTO.class))),
                    @ApiResponse(responseCode = "404", description = "Ruta no encontrada")
            }
    )
    @GetMapping("/{id}")
    public ResponseEntity<RutaResponseDTO> obtenerPorId(
            @Parameter(description = "ID de la ruta")
            @PathVariable UUID id) {
        return ResponseEntity.ok(planificacionService.obtenerRuta(id));
    }

    @Operation(
            summary = "Iniciar ruta",
            description = "El chofer indica el inicio de la ruta: todas sus entregas pasan a EN_TRASLADO y se publica el evento a n8n.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Ruta iniciada"),
                    @ApiResponse(responseCode = "404", description = "Ruta no encontrada")
            }
    )
    @PostMapping("/{id}/iniciar")
    public ResponseEntity<Void> iniciar(
            @Parameter(description = "ID de la ruta")
            @PathVariable UUID id) {
        planificacionService.iniciarRuta(id);
        return ResponseEntity.ok().build();
    }
}
