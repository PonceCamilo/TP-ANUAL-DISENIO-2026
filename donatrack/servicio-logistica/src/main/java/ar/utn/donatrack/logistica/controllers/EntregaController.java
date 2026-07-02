package ar.utn.donatrack.logistica.controllers;

import ar.utn.donatrack.logistica.dtos.request.ConfirmarEntregaRequestDTO;
import ar.utn.donatrack.logistica.dtos.request.NoRecibidaRequestDTO;
import ar.utn.donatrack.logistica.dtos.response.EntregaResponseDTO;
import ar.utn.donatrack.logistica.interfaces.services.EntregaServiceInterface;
import ar.utn.donatrack.logistica.models.entrega.EstadoEntrega;
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

@RestController
@RequestMapping("/api/logistica/entregas")
@RequiredArgsConstructor
@Tag(name = "Entregas", description = "Consulta y cambios de estado de las entregas")
public class EntregaController {

    private final EntregaServiceInterface entregaService;

    @Operation(
            summary = "Obtener entrega por ID",
            description = "Devuelve el detalle de una entrega, incluyendo su estado actual e historial.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Entrega encontrada",
                            content = @Content(schema = @Schema(implementation = EntregaResponseDTO.class))),
                    @ApiResponse(responseCode = "404", description = "Entrega no encontrada")
            }
    )
    @GetMapping("/{id}")
    public ResponseEntity<EntregaResponseDTO> obtenerPorId(
            @Parameter(description = "ID de la entrega", example = "11111111-1111-1111-1111-111111111111")
            @PathVariable UUID id) {
        return ResponseEntity.ok(entregaService.obtenerPorId(id));
    }

    @Operation(
            summary = "Listar entregas por estado",
            description = "Listado para revisión administrativa, por ejemplo las entregas en estado NO_RECIBIDA.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Entregas del estado indicado",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = EntregaResponseDTO.class))))
            }
    )
    @GetMapping
    public ResponseEntity<List<EntregaResponseDTO>> obtenerPorEstado(
            @Parameter(description = "Estado por el que filtrar", example = "NO_RECIBIDA")
            @RequestParam EstadoEntrega estado) {
        return ResponseEntity.ok(entregaService.obtenerPorEstado(estado));
    }

    @Operation(
            summary = "Confirmar recepción de la entrega",
            description = "La entidad beneficiaria confirma la recepción. La entrega pasa a ENTREGADA y se publica el evento a n8n.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Entrega confirmada"),
                    @ApiResponse(responseCode = "409", description = "Transición de estado inválida")
            }
    )
    @PostMapping("/{id}/confirmar")
    public ResponseEntity<Void> confirmar(
            @Parameter(description = "ID de la entrega", example = "11111111-1111-1111-1111-111111111111")
            @PathVariable UUID id, @Valid @RequestBody ConfirmarEntregaRequestDTO dto) {
        entregaService.confirmar(id, dto);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Marcar entrega como no recibida",
            description = "La entidad informa que no recibió la entrega. Pasa a NO_RECIBIDA y n8n recibe el evento con requiereAvisoAdmin=true.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Entrega marcada como no recibida"),
                    @ApiResponse(responseCode = "409", description = "Transición de estado inválida")
            }
    )
    @PostMapping("/{id}/no-recibida")
    public ResponseEntity<Void> marcarNoRecibida(
            @Parameter(description = "ID de la entrega", example = "11111111-1111-1111-1111-111111111111")
            @PathVariable UUID id, @Valid @RequestBody NoRecibidaRequestDTO dto) {
        entregaService.marcarNoRecibida(id, dto);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Regresar la donación al depósito",
            description = "La donación regresa al depósito tras una entrega no recibida. La entrega vuelve a PENDIENTE.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Entrega regresada a depósito"),
                    @ApiResponse(responseCode = "409", description = "Transición de estado inválida")
            }
    )
    @PostMapping("/{id}/regreso-deposito")
    public ResponseEntity<Void> regresarADeposito(
            @Parameter(description = "ID de la entrega", example = "11111111-1111-1111-1111-111111111111")
            @PathVariable UUID id) {
        entregaService.regresarADeposito(id);
        return ResponseEntity.ok().build();
    }
}
