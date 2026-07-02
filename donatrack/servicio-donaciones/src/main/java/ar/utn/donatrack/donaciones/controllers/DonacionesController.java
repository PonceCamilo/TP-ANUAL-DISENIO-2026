package ar.utn.donatrack.donaciones.controllers;

import ar.utn.donatrack.donaciones.dtos.request.AsignacionRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.BienRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.CambioEstadoRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.DonacionRequestDTO;
import ar.utn.donatrack.donaciones.models.donacion.CargaDonacion;
import ar.utn.donatrack.donaciones.dtos.response.CandidatosAsignacionResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.DonacionResponseDTO;
import ar.utn.donatrack.donaciones.interfaces.services.DonacionServiceInterface;
import ar.utn.donatrack.donaciones.interfaces.services.SegmentadorDonacionesServiceInterface;
import ar.utn.donatrack.donaciones.mappers.DonacionMapper;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.validations.personas.PersonasValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/donaciones")
@Validated
@Tag(name = "Donaciones", description = "Registro, consulta, cambios de estado y asignación de donaciones a entidades beneficiarias")
public class DonacionesController {

  private final DonacionServiceInterface donacionService;
  private final SegmentadorDonacionesServiceInterface segmentadorDonacionesService;
  private final PersonasValidator personasValidator;
  private final DonacionMapper mapper;

  @Operation(
      summary = "Listar donaciones",
      description = "Devuelve las donaciones registradas, con filtros opcionales por estado, donante y subcategoría.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Donaciones encontradas",
              content = @Content(array = @ArraySchema(schema = @Schema(implementation = DonacionResponseDTO.class))))
      }
  )
  @GetMapping
  public ResponseEntity<List<DonacionResponseDTO>> obtenerDonaciones(
      @Parameter(description = "Nombre del estado de la donación", example = "EN_DEPOSITO")
      @RequestParam(required = false) String estado,
      @Parameter(description = "ID de la persona donante")
      @RequestParam(required = false) UUID idDonante,
      @Parameter(description = "Subcategoría de los bienes", example = "Fideos secos")
      @RequestParam(required = false) String subcategoria
  ) {
    return ResponseEntity.ok(donacionService.obtenerDonaciones(estado, idDonante, subcategoria));
  }

  @Operation(
      summary = "Obtener una donación por ID",
      description = "Devuelve el detalle completo de una donación, incluyendo su historial de cambios de estado.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Donación encontrada",
              content = @Content(schema = @Schema(implementation = DonacionResponseDTO.class))),
          @ApiResponse(responseCode = "404", description = "Donación no encontrada")
      }
  )
  @GetMapping("/{id}")
  public ResponseEntity<DonacionResponseDTO> obtenerDonacion(
      @Parameter(description = "ID de la donación") @PathVariable UUID id
  ) {
    return ResponseEntity.ok(donacionService.obtenerPorId(id));
  }

  @Operation(
      summary = "Registrar una donación",
      description = "Recibe la carga de bienes de un donante y la segmenta automáticamente en una o más donaciones agrupadas por subcategoría.",
      responses = {
          @ApiResponse(responseCode = "201", description = "Donaciones segmentadas y creadas",
              content = @Content(array = @ArraySchema(schema = @Schema(implementation = DonacionResponseDTO.class)))),
          @ApiResponse(responseCode = "400", description = "Datos inválidos"),
          @ApiResponse(responseCode = "404", description = "Persona donante no encontrada")
      }
  )
  @PostMapping
  public ResponseEntity<List<DonacionResponseDTO>> registrar(
      @RequestBody @Valid DonacionRequestDTO dto
  ) {
    personasValidator.validarExistenciaPersona(dto.getIdDonante());
    CargaDonacion carga = mapper.toCargaDonacion(dto);
    List<Donacion> donacionesCreadas = segmentadorDonacionesService.segmentar(carga);
    return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toDTOList(donacionesCreadas));
  }

  @Operation(
      summary = "Cambiar el estado de una donación",
      description = "Transiciona la donación al estado indicado, validando que la transición sea legal según el estado actual. Registra el cambio en el historial con el estado previo y el nombre de la transición.",
      responses = {
          @ApiResponse(responseCode = "204", description = "Estado actualizado"),
          @ApiResponse(responseCode = "400", description = "Falta justificación para la transición"),
          @ApiResponse(responseCode = "404", description = "Donación no encontrada"),
          @ApiResponse(responseCode = "422", description = "Transición de estado no permitida")
      }
  )
  @PatchMapping("/{id}/estado")
  public ResponseEntity<Void> cambiarEstadoDonacion(
      @Parameter(description = "ID de la donación") @PathVariable UUID id,
      @RequestBody @Valid CambioEstadoRequestDTO dto
  ) {
    donacionService.cambiarEstado(id, dto);
    return ResponseEntity.noContent().build();
  }

  @Operation(
      summary = "Modificar el bien de una donación",
      description = "Reemplaza el primer bien registrado en la donación.",
      responses = {
          @ApiResponse(responseCode = "204", description = "Bien actualizado"),
          @ApiResponse(responseCode = "404", description = "Donación no encontrada"),
          @ApiResponse(responseCode = "409", description = "La donación no tiene bienes para modificar")
      }
  )
  @PatchMapping("/{id}/bien")
  public ResponseEntity<Void> modificarBienDonacion(
      @Parameter(description = "ID de la donación") @PathVariable UUID id,
      @RequestBody @Valid BienRequestDTO dto
  ) {
    donacionService.modificarBien(id, dto);
    return ResponseEntity.noContent().build();
  }

  @Operation(
      summary = "Obtener ranking de entidades candidatas",
      description = "Ejecuta los algoritmos de compatibilidad semántica y prioridad a sub-atendidos, devolviendo el ranking de entidades beneficiarias candidatas a recibir la donación.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Rankings calculados",
              content = @Content(schema = @Schema(implementation = CandidatosAsignacionResponseDTO.class))),
          @ApiResponse(responseCode = "404", description = "Donación no encontrada")
      }
  )
  @GetMapping("/{id}/candidatos")
  public ResponseEntity<CandidatosAsignacionResponseDTO> obtenerCandidatos(
      @Parameter(description = "ID de la donación") @PathVariable UUID id
  ) {
    return ResponseEntity.ok(donacionService.obtenerCandidatos(id));
  }

  @Operation(
      summary = "Asignar la donación a una entidad beneficiaria",
      description = "Confirma la entidad beneficiaria destino elegida (a partir del ranking de candidatos) y transiciona la donación a ASIGNACION_REALIZADA.",
      responses = {
          @ApiResponse(responseCode = "204", description = "Donación asignada"),
          @ApiResponse(responseCode = "404", description = "Donación o entidad beneficiaria no encontrada")
      }
  )
  @PatchMapping("/{id}/asignar")
  public ResponseEntity<Void> asignarEntidad(
      @Parameter(description = "ID de la donación") @PathVariable UUID id,
      @RequestBody @Valid AsignacionRequestDTO dto
  ) {
    donacionService.asignar(id, dto);
    return ResponseEntity.noContent().build();
  }

  @Operation(
      summary = "Eliminar una donación",
      responses = {
          @ApiResponse(responseCode = "204", description = "Donación eliminada"),
          @ApiResponse(responseCode = "404", description = "Donación no encontrada")
      }
  )
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminarDonacion(
      @Parameter(description = "ID de la donación") @PathVariable UUID id
  ) {
    donacionService.eliminar(id);
    return ResponseEntity.noContent().build();
  }
}
