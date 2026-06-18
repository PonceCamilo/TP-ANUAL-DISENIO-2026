package ar.utn.donatrack.donaciones.controllers;

import ar.utn.donatrack.donaciones.dtos.request.CampaniaRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.EntidadBeneficiariaRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.NecesidadRequestDTO;
import ar.utn.donatrack.donaciones.dtos.response.EntidadBeneficiariaResponseDTO;
import ar.utn.donatrack.donaciones.interfaces.services.EntidadesBeneficiariasServiceInterface;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/entidades")
@Validated
public class EntidadBeneficiariaController {

    private final EntidadesBeneficiariasServiceInterface entidadesBeneficiariasService;

    @PostMapping
    public ResponseEntity<Void> crearEntidad(
            @RequestBody @Valid EntidadBeneficiariaRequestDTO dto
    ) {
        UUID id = entidadesBeneficiariasService.guardar(dto);
        URI location = URI.create("/entidades/" + id);
        return ResponseEntity.created(location).build();
    }

    @GetMapping
    public ResponseEntity<List<EntidadBeneficiariaResponseDTO>> obtenerTodas() {
        return ResponseEntity.ok(entidadesBeneficiariasService.obtenerTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntidadBeneficiariaResponseDTO> obtenerPorId(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(entidadesBeneficiariasService.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> actualizarEntidad(
            @PathVariable UUID id,
            @RequestBody @Valid EntidadBeneficiariaRequestDTO dto
    ) {
        entidadesBeneficiariasService.actualizar(id, dto);
        return ResponseEntity.noContent().build();
    }

    // ── ENDPOINTS NUEVOS PARA CUMPLIR EL CRUD Y REQUISITOS ──

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarEntidad(
            @PathVariable UUID id
    ) {
        entidadesBeneficiariasService.eliminarEntidad(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/campanias")
    public ResponseEntity<Void> crearCampania(
            @PathVariable UUID id,
            @RequestBody @Valid CampaniaRequestDTO dto
    ) {
        UUID campaniaId = entidadesBeneficiariasService.agregarCampaniaAEntidad(id, dto);
        URI location = URI.create("/entidades/" + id + "/campanias/" + campaniaId);
        return ResponseEntity.created(location).build();
    }

    @PostMapping("/{entidadId}/campanias/{campaniaId}/necesidades")
    public ResponseEntity<Void> agregarNecesidadACampania(
            @PathVariable UUID entidadId,
            @PathVariable UUID campaniaId,
            @RequestBody @Valid NecesidadRequestDTO dto // <--- ACA AHORA SE RECIBE EL DTO, NO EL MODELO
    ) {
        entidadesBeneficiariasService.agregarNecesidadACampania(entidadId, campaniaId, dto);
        return ResponseEntity.status(201).build(); // 201 Created es lo correcto al crear una Necesidad
    }
}