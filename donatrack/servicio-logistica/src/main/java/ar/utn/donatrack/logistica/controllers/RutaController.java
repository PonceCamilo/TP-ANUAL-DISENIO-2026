package ar.utn.donatrack.logistica.controllers;

import ar.utn.donatrack.logistica.dtos.response.RutaResponseDTO;
import ar.utn.donatrack.logistica.interfaces.services.PlanificacionServiceInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/logistica/rutas")
@RequiredArgsConstructor
public class RutaController {

    private final PlanificacionServiceInterface planificacionService;

    @GetMapping("/{id}")
    public ResponseEntity<RutaResponseDTO> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(planificacionService.obtenerRuta(id));
    }

    /** El chofer indica el inicio de la ruta: todas sus entregas pasan a EN_TRASLADO. */
    @PostMapping("/{id}/iniciar")
    public ResponseEntity<Void> iniciar(@PathVariable UUID id) {
        planificacionService.iniciarRuta(id);
        return ResponseEntity.ok().build();
    }
}
