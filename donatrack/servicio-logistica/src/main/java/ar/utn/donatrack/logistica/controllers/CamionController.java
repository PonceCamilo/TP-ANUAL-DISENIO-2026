package ar.utn.donatrack.logistica.controllers;

import ar.utn.donatrack.logistica.dtos.request.CamionRequestDTO;
import ar.utn.donatrack.logistica.dtos.response.CamionResponseDTO;
import ar.utn.donatrack.logistica.dtos.response.RutaResponseDTO;
import ar.utn.donatrack.logistica.interfaces.services.CamionServiceInterface;
import ar.utn.donatrack.logistica.interfaces.services.PlanificacionServiceInterface;
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
public class CamionController {

    private final CamionServiceInterface camionService;
    private final PlanificacionServiceInterface planificacionService;

    @PostMapping
    public ResponseEntity<CamionResponseDTO> registrar(@Valid @RequestBody CamionRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(camionService.registrar(dto));
    }

    @GetMapping
    public ResponseEntity<List<CamionResponseDTO>> obtenerTodos() {
        return ResponseEntity.ok(camionService.obtenerTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CamionResponseDTO> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(camionService.obtenerPorId(id));
    }

    /** Ruta vigente del camión, consumida por la app del chofer. */
    @GetMapping("/{id}/ruta")
    public ResponseEntity<RutaResponseDTO> obtenerRutaVigente(@PathVariable UUID id) {
        return ResponseEntity.ok(planificacionService.obtenerRutaVigentePorCamion(id));
    }
}
