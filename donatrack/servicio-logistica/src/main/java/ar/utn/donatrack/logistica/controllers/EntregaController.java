package ar.utn.donatrack.logistica.controllers;

import ar.utn.donatrack.logistica.dtos.request.ConfirmarEntregaRequestDTO;
import ar.utn.donatrack.logistica.dtos.request.NoRecibidaRequestDTO;
import ar.utn.donatrack.logistica.dtos.response.EntregaResponseDTO;
import ar.utn.donatrack.logistica.interfaces.services.EntregaServiceInterface;
import ar.utn.donatrack.logistica.models.entrega.EstadoEntrega;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/logistica/entregas")
@RequiredArgsConstructor
public class EntregaController {

    private final EntregaServiceInterface entregaService;

    @GetMapping("/{id}")
    public ResponseEntity<EntregaResponseDTO> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(entregaService.obtenerPorId(id));
    }

    /** Listado para revisión administrativa, ej. ?estado=NO_RECIBIDA */
    @GetMapping
    public ResponseEntity<List<EntregaResponseDTO>> obtenerPorEstado(@RequestParam EstadoEntrega estado) {
        return ResponseEntity.ok(entregaService.obtenerPorEstado(estado));
    }

    /** La entidad beneficiaria confirma la recepción de la entrega. */
    @PostMapping("/{id}/confirmar")
    public ResponseEntity<Void> confirmar(@PathVariable UUID id, @Valid @RequestBody ConfirmarEntregaRequestDTO dto) {
        entregaService.confirmar(id, dto);
        return ResponseEntity.ok().build();
    }

    /** La entidad informa que no recibió la entrega. */
    @PostMapping("/{id}/no-recibida")
    public ResponseEntity<Void> marcarNoRecibida(@PathVariable UUID id, @Valid @RequestBody NoRecibidaRequestDTO dto) {
        entregaService.marcarNoRecibida(id, dto);
        return ResponseEntity.ok().build();
    }

    /** La donación regresa al depósito tras una entrega no recibida. */
    @PostMapping("/{id}/regreso-deposito")
    public ResponseEntity<Void> regresarADeposito(@PathVariable UUID id) {
        entregaService.regresarADeposito(id);
        return ResponseEntity.ok().build();
    }
}
