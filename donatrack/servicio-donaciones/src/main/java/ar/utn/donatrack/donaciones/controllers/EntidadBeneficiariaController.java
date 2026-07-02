package ar.utn.donatrack.donaciones.controllers;

import ar.utn.donatrack.donaciones.dtos.request.CampaniaRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.EntidadBeneficiariaRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.NecesidadRequestDTO;
import ar.utn.donatrack.donaciones.dtos.response.EntidadBeneficiariaResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.NecesidadResponseDTO;
import ar.utn.donatrack.donaciones.interfaces.services.EntidadesBeneficiariasServiceInterface;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/entidades")
@Validated
@Tag(name = "Entidades Beneficiarias", description = "Gestión de entidades beneficiarias, sus campañas y necesidades materiales")
public class EntidadBeneficiariaController {

    private final EntidadesBeneficiariasServiceInterface entidadesBeneficiariasService;

    @Operation(
        summary = "Crear una entidad beneficiaria",
        responses = {
            @ApiResponse(responseCode = "201", description = "Entidad creada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos")
        }
    )
    @PostMapping
    public ResponseEntity<Void> crearEntidad(
            @RequestBody @Valid EntidadBeneficiariaRequestDTO dto
    ) {
        UUID id = entidadesBeneficiariasService.guardar(dto);
        URI location = URI.create("/entidades/" + id);
        return ResponseEntity.created(location).build();
    }

    @Operation(
        summary = "Listar entidades beneficiarias",
        responses = {
            @ApiResponse(responseCode = "200", description = "Entidades encontradas",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = EntidadBeneficiariaResponseDTO.class))))
        }
    )
    @GetMapping
    public ResponseEntity<List<EntidadBeneficiariaResponseDTO>> obtenerTodas() {
        return ResponseEntity.ok(entidadesBeneficiariasService.obtenerTodas());
    }

    @Operation(
        summary = "Obtener una entidad beneficiaria por ID",
        responses = {
            @ApiResponse(responseCode = "200", description = "Entidad encontrada",
                content = @Content(schema = @Schema(implementation = EntidadBeneficiariaResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Entidad no encontrada")
        }
    )
    @GetMapping("/{id}")
    public ResponseEntity<EntidadBeneficiariaResponseDTO> obtenerPorId(
            @Parameter(description = "ID de la entidad beneficiaria") @PathVariable UUID id
    ) {
        return ResponseEntity.ok(entidadesBeneficiariasService.obtenerPorId(id));
    }

    @Operation(
        summary = "Actualizar una entidad beneficiaria",
        responses = {
            @ApiResponse(responseCode = "204", description = "Entidad actualizada"),
            @ApiResponse(responseCode = "404", description = "Entidad no encontrada")
        }
    )
    @PutMapping("/{id}")
    public ResponseEntity<Void> actualizarEntidad(
            @Parameter(description = "ID de la entidad beneficiaria") @PathVariable UUID id,
            @RequestBody @Valid EntidadBeneficiariaRequestDTO dto
    ) {
        entidadesBeneficiariasService.actualizar(id, dto);
        return ResponseEntity.noContent().build();
    }

    // ── ENDPOINTS NUEVOS PARA CUMPLIR EL CRUD Y REQUISITOS ──

    @Operation(
        summary = "Eliminar una entidad beneficiaria",
        responses = {
            @ApiResponse(responseCode = "204", description = "Entidad eliminada"),
            @ApiResponse(responseCode = "404", description = "Entidad no encontrada")
        }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarEntidad(
            @Parameter(description = "ID de la entidad beneficiaria") @PathVariable UUID id
    ) {
        entidadesBeneficiariasService.eliminarEntidad(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Crear una campaña para una entidad",
        description = "Agrupa múltiples necesidades bajo una misma campaña (ej: tras una inundación).",
        responses = {
            @ApiResponse(responseCode = "201", description = "Campaña creada"),
            @ApiResponse(responseCode = "400", description = "Fechas de campaña inválidas"),
            @ApiResponse(responseCode = "404", description = "Entidad no encontrada")
        }
    )
    @PostMapping("/{id}/campanias")
    public ResponseEntity<Void> crearCampania(
            @Parameter(description = "ID de la entidad beneficiaria") @PathVariable UUID id,
            @RequestBody @Valid CampaniaRequestDTO dto
    ) {
        UUID campaniaId = entidadesBeneficiariasService.agregarCampaniaAEntidad(id, dto);
        URI location = URI.create("/entidades/" + id + "/campanias/" + campaniaId);
        return ResponseEntity.created(location).build();
    }

    @Operation(
        summary = "Agregar una necesidad a una campaña",
        description = "Registra una necesidad material (recurrente o extraordinaria) dentro de una campaña existente de la entidad.",
        responses = {
            @ApiResponse(responseCode = "201", description = "Necesidad creada",
                content = @Content(schema = @Schema(implementation = NecesidadResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Entidad o campaña no encontrada")
        }
    )
    @PostMapping("/{entidadId}/campanias/{campaniaId}/necesidades")
    public ResponseEntity<NecesidadResponseDTO> agregarNecesidadACampania(
            @Parameter(description = "ID de la entidad beneficiaria") @PathVariable UUID entidadId,
            @Parameter(description = "ID de la campaña") @PathVariable UUID campaniaId,
            @RequestBody @Valid NecesidadRequestDTO dto
    ) {
        NecesidadResponseDTO necesidadCreada =
                entidadesBeneficiariasService.agregarNecesidadACampania(entidadId, campaniaId, dto);
        URI location = URI.create(
                "/entidades/" + entidadId + "/campanias/" + campaniaId + "/necesidades/" + necesidadCreada.getId());
        return ResponseEntity.created(location).body(necesidadCreada);
    }
}
