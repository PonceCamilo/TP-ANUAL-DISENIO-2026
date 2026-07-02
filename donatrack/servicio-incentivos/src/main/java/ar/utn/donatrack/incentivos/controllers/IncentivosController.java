package ar.utn.donatrack.incentivos.controllers;

import ar.utn.donatrack.incentivos.dtos.request.ProcesarDonacionRequest;
import ar.utn.donatrack.incentivos.dtos.request.ProcesarDonacionExitosaRequest;
import ar.utn.donatrack.incentivos.dtos.response.InsigniaResponse;
import ar.utn.donatrack.incentivos.dtos.response.MetricasDonanteResponse;
import ar.utn.donatrack.incentivos.dtos.response.MisionResponse;
import ar.utn.donatrack.incentivos.models.Donante;
import ar.utn.donatrack.incentivos.interfaces.services.IncentivosServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

// El @Tag y los @Operation sirven para la docu en Swagger . el tag agrupa los endpoints y el operation pone titulo, descripcion y rta esperada para cada endpoint

@RestController
@RequestMapping("/api/incentivos/donantes")
@Tag(name = "Incentivos", description = "Operaciones del servicio de incentivos de DonaTrack")
public class IncentivosController {

    private final IncentivosServiceInterface service;

    public IncentivosController(IncentivosServiceInterface service) {
        this.service = service;
    }

    @Operation(
            summary = "Obtener metricas del donante",
            description = "Devuelve categoria actual, donaciones acumuladas, donaciones del mes, organizaciones ayudadas y posicion de ranking.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Metricas calculadas",
                            content = @Content(schema = @Schema(implementation = MetricasDonanteResponse.class)))
            }
    )
    @GetMapping("/{id}/metricas")
    public ResponseEntity<MetricasDonanteResponse> obtenerMetricas(
            @Parameter(description = "ID del donante", example = "11111111-1111-1111-1111-111111111111")
            @PathVariable UUID id) {
        Donante donante = service.obtenerMetricas(id);
        return ResponseEntity.ok(MetricasDonanteResponse.desde(donante, service.obtenerPosicionRankingActual(id)));
    }

    @Operation(
            summary = "Listar misiones disponibles",
            description = "Devuelve las misiones de la categoria actual del donante junto con su progreso calculado.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Misiones del donante",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = MisionResponse.class))))
            }
    )
    @GetMapping("/{id}/misiones")
    public ResponseEntity<List<MisionResponse>> obtenerMisiones(
            @Parameter(description = "ID del donante", example = "11111111-1111-1111-1111-111111111111")
            @PathVariable UUID id) {
        Donante donante = service.obtenerMetricas(id);
        List<MisionResponse> misiones = service.obtenerMisiones(id)
                .stream()
                .map(m -> MisionResponse.desde(m, donante))
                .toList();
        return ResponseEntity.ok(misiones);
    }

    @Operation(
            summary = "Listar insignias obtenidas",
            description = "Devuelve las insignias ganadas por el donante y su visibilidad.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Insignias del donante",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = InsigniaResponse.class))))
            }
    )
    @GetMapping("/{id}/insignias")
    public ResponseEntity<List<InsigniaResponse>> obtenerInsignias(
            @Parameter(description = "ID del donante", example = "11111111-1111-1111-1111-111111111111")
            @PathVariable UUID id) {
        List<InsigniaResponse> insignias = service.obtenerInsignias(id)
                .stream()
                .map(InsigniaResponse::desde)
                .toList();
        return ResponseEntity.ok(insignias);
    }

    @Operation(
            summary = "Procesar donacion registrada",
            description = "Endpoint de integracion usado por servicio-donaciones cuando se registra una donacion. Actualiza el historial y el progreso de misiones.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Evento procesado",
                            content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode = "400", description = "Request invalido")
            }
    )
    @PostMapping("/donacion")
    public ResponseEntity<String> procesarDonacion(@Valid @RequestBody ProcesarDonacionRequest request) {
        service.procesarDonacion(
                request.donanteId(),
                request.destinatario(),
                request.medio(),
                request.cantidadBienes(),
                request.categoriasDonadas()
        );
        return ResponseEntity.ok("Donacion registrada en incentivos para el donante " + request.donanteId());
    }

    @Operation(
            summary = "Procesar donacion exitosa",
            description = "Endpoint de integracion usado por servicio-donaciones cuando una donacion fue entregada. Puede completar misiones y disparar n8n.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Evento procesado",
                            content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode = "400", description = "Request invalido")
            }
    )
    @PostMapping("/donacion-exitosa")
    public ResponseEntity<String> procesarDonacionExitosa(@Valid @RequestBody ProcesarDonacionExitosaRequest request) {
        service.procesarDonacionExitosa(
                request.donanteId(),
                request.destinatario(),
                request.medio()
        );
        return ResponseEntity.ok("Donacion exitosa registrada en incentivos para el donante " + request.donanteId());
    }
}
