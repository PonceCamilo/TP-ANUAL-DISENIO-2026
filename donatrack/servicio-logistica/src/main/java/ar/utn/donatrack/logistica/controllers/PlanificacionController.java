package ar.utn.donatrack.logistica.controllers;

import ar.utn.donatrack.logistica.dtos.request.CallbackRutaRequestDTO;
import ar.utn.donatrack.logistica.dtos.request.PlanificacionRequestDTO;
import ar.utn.donatrack.logistica.dtos.response.LoteResponseDTO;
import ar.utn.donatrack.logistica.interfaces.services.PlanificacionServiceInterface;
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
public class PlanificacionController {

    private final PlanificacionServiceInterface planificacionService;

    @PostMapping
    public ResponseEntity<List<LoteResponseDTO>> planificar(@Valid @RequestBody PlanificacionRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(planificacionService.planificar(dto));
    }

    @GetMapping("/{loteId}")
    public ResponseEntity<LoteResponseDTO> obtenerLote(@PathVariable UUID loteId) {
        return ResponseEntity.ok(planificacionService.obtenerLote(loteId));
    }

    @PostMapping("/callback")
    public ResponseEntity<Void> callback(@Valid @RequestBody CallbackRutaRequestDTO dto) {
        planificacionService.registrarCallback(dto);
        return ResponseEntity.ok().build();
    }
}
