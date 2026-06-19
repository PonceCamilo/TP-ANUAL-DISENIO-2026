package ar.utn.donatrack.incentivos.controllers;

import ar.utn.donatrack.incentivos.dtos.request.ProcesarDonacionRequest;
import ar.utn.donatrack.incentivos.dtos.response.InsigniaResponse;
import ar.utn.donatrack.incentivos.dtos.response.MetricasDonanteResponse;
import ar.utn.donatrack.incentivos.dtos.response.MisionResponse;
import ar.utn.donatrack.incentivos.models.Donante;
import ar.utn.donatrack.incentivos.services.IncentivosService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * API REST del Servicio de Incentivos.
 *
 * GET  /api/incentivos/donantes/{id}/metricas   → métricas del donante
 * GET  /api/incentivos/donantes/{id}/misiones   → misiones disponibles
 * GET  /api/incentivos/donantes/{id}/insignias  → insignias obtenidas
 *
 * POST /api/incentivos/donantes/donacion         → (para integración con servicio-donaciones)
 * POST /api/incentivos/donantes/donacion-exitosa → (para integración con servicio-donaciones)
 */
@RestController
@RequestMapping("/api/incentivos/donantes")
public class IncentivosController {

    private final IncentivosService service;

    public IncentivosController(IncentivosService service) {
        this.service = service;
    }

    // ── Endpoints de consulta (requeridos por la consigna) ───────────────────

    @GetMapping("/{id}/metricas")
    public ResponseEntity<MetricasDonanteResponse> obtenerMetricas(@PathVariable UUID id) {
        return ResponseEntity.ok(
                MetricasDonanteResponse.desde(service.obtenerMetricas(id))
        );
    }

    @GetMapping("/{id}/misiones")
    public ResponseEntity<List<MisionResponse>> obtenerMisiones(@PathVariable UUID id) {
        Donante donante = service.obtenerMetricas(id);

        List<MisionResponse> misiones = service.obtenerMisiones(id)
                .stream()
                .map(m -> MisionResponse.desde(m, donante)) 
                .toList();
        return ResponseEntity.ok(misiones);
    }

    @GetMapping("/{id}/insignias")
    public ResponseEntity<List<InsigniaResponse>> obtenerInsignias(@PathVariable UUID id) {
        List<InsigniaResponse> insignias = service.obtenerInsignias(id)
                .stream()
                .map(InsigniaResponse::desde)
                .toList();
        return ResponseEntity.ok(insignias);
    }

    // ── Endpoints de integración (llamados por servicio-donaciones) ───────────

    /**
     * servicio-donaciones llama a este endpoint cada vez que un donante registra una donación.
     * Aquí se actualizan métricas y se disparan notificaciones si se completan misiones.
     */
    @PostMapping("/donacion")
    public ResponseEntity<Void> procesarDonacion(
            @Valid @RequestBody ProcesarDonacionRequest request) {
        service.procesarDonacion(
                request.donanteId(),
                request.destinatario(),
                request.medio(),
                request.cantidadBienes(),
                request.categoriasDonadas()
        );
        return ResponseEntity.ok().build();
    }

    /**
     * servicio-donaciones llama a este endpoint cuando una donación fue entregada exitosamente.
     */
    @PostMapping("/donacion-exitosa")
    public ResponseEntity<Void> procesarDonacionExitosa(
            @Valid @RequestBody ProcesarDonacionRequest request) {
        service.procesarDonacionExitosa(
                request.donanteId(),
                request.destinatario(),
                request.medio()
        );
        return ResponseEntity.ok().build();
    }
}


