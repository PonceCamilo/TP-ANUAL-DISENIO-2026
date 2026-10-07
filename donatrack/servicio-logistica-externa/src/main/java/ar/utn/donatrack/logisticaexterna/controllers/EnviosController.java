package ar.utn.donatrack.logisticaexterna.controllers;

import ar.utn.donatrack.logisticaexterna.dtos.CrearEnvioRequest;
import ar.utn.donatrack.logisticaexterna.dtos.DespacharRequest;
import ar.utn.donatrack.logisticaexterna.dtos.EnvioResponse;
import ar.utn.donatrack.logisticaexterna.dtos.FallarRequest;
import ar.utn.donatrack.logisticaexterna.services.EnvioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API del proveedor externo. Ciclo de un envío:
 *   POST /api/v1/envios                         → ACEPTADO (devuelve trackingId)
 *   POST /api/v1/envios/{trackingId}/despacho   → EN_CAMINO
 *   POST /api/v1/envios/{trackingId}/entrega    → ENTREGADO
 *   POST /api/v1/envios/{trackingId}/fallo      → FALLIDO (se puede volver a despachar)
 * Cada cambio de estado se avisa por webhook (ver WebhookEventosNotifier).
 */
@RestController
@RequestMapping("/api/v1/envios")
@RequiredArgsConstructor
@Tag(name = "Envíos", description = "Alta, tracking y ciclo de vida de envíos")
public class EnviosController {

    private final EnvioService envioService;

    @Operation(
            summary = "Crear envío",
            description = "Registra un envío (uno por request) y devuelve su trackingId. Queda en estado ACEPTADO.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Envío aceptado"),
                    @ApiResponse(responseCode = "400", description = "Request inválido")
            }
    )
    @PostMapping
    public ResponseEntity<EnvioResponse> crear(@Valid @RequestBody CrearEnvioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(envioService.crear(request));
    }

    @Operation(summary = "Listar envíos")
    @GetMapping
    public ResponseEntity<List<EnvioResponse>> listar() {
        return ResponseEntity.ok(envioService.listar());
    }

    @Operation(
            summary = "Consultar envío",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Envío encontrado"),
                    @ApiResponse(responseCode = "404", description = "Envío no encontrado")
            }
    )
    @GetMapping("/{trackingId}")
    public ResponseEntity<EnvioResponse> consultar(@PathVariable String trackingId) {
        return ResponseEntity.ok(envioService.consultar(trackingId));
    }

    @Operation(
            summary = "Despachar envío",
            description = "El envío sale en el vehículo indicado (EN_CAMINO). También sirve para reintentar un envío FALLIDO.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Envío despachado"),
                    @ApiResponse(responseCode = "404", description = "Envío no encontrado"),
                    @ApiResponse(responseCode = "409", description = "El envío no está en un estado que permita despacharlo")
            }
    )
    @PostMapping("/{trackingId}/despacho")
    public ResponseEntity<EnvioResponse> despachar(@PathVariable String trackingId,
                                                   @Valid @RequestBody DespacharRequest request) {
        return ResponseEntity.ok(envioService.despachar(trackingId, request.vehiculo()));
    }

    @Operation(
            summary = "Confirmar entrega",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Envío entregado"),
                    @ApiResponse(responseCode = "404", description = "Envío no encontrado"),
                    @ApiResponse(responseCode = "409", description = "El envío no está EN_CAMINO")
            }
    )
    @PostMapping("/{trackingId}/entrega")
    public ResponseEntity<EnvioResponse> entregar(@PathVariable String trackingId) {
        return ResponseEntity.ok(envioService.entregar(trackingId));
    }

    @Operation(
            summary = "Informar entrega fallida",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Envío marcado como FALLIDO"),
                    @ApiResponse(responseCode = "404", description = "Envío no encontrado"),
                    @ApiResponse(responseCode = "409", description = "El envío no está EN_CAMINO")
            }
    )
    @PostMapping("/{trackingId}/fallo")
    public ResponseEntity<EnvioResponse> fallar(@PathVariable String trackingId,
                                                @Valid @RequestBody FallarRequest request) {
        return ResponseEntity.ok(envioService.fallar(trackingId, request.motivo()));
    }
}
