package ar.utn.donatrack.donaciones.controllers;

import ar.utn.donatrack.donaciones.dtos.request.EstadoDonanteRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.MedioDeContactoRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.PersonaDonanteRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.PersonaDonanteUpdateRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.RepresentanteRequestDTO;
import ar.utn.donatrack.donaciones.dtos.response.PersonaDonanteResponseDTO;
import ar.utn.donatrack.donaciones.importacion.ImportReport;
import ar.utn.donatrack.donaciones.interfaces.services.PersonaDonanteServiceInterface;
import ar.utn.donatrack.donaciones.models.donante.EstadoDonante;
import ar.utn.donatrack.donaciones.services.CsvImportService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/donantes")
@Validated
@Tag(name = "Personas Donantes", description = "Gestión de personas donantes (humanas y jurídicas), su estado y la importación masiva por CSV")
public class PersonaDonanteController {

  private final PersonaDonanteServiceInterface personaDonanteService;
  private final CsvImportService csvImportService;

  @Operation(
      summary = "Listar personas donantes",
      description = "Devuelve las personas donantes registradas, con filtro opcional por estado.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Donantes encontrados",
              content = @Content(array = @ArraySchema(schema = @Schema(implementation = PersonaDonanteResponseDTO.class))))
      }
  )
  @GetMapping
  public ResponseEntity<List<PersonaDonanteResponseDTO>> obtenerDonantes(
      @Parameter(description = "Estado del donante") @RequestParam(required = false) EstadoDonante estado
  ) {
    return ResponseEntity.ok(personaDonanteService.obtenerDonantes(estado));
  }

  @Operation(
      summary = "Obtener una persona donante por ID",
      responses = {
          @ApiResponse(responseCode = "200", description = "Donante encontrado",
              content = @Content(schema = @Schema(implementation = PersonaDonanteResponseDTO.class))),
          @ApiResponse(responseCode = "404", description = "Donante no encontrado")
      }
  )
  @GetMapping("/{id}")
  public ResponseEntity<PersonaDonanteResponseDTO> obtenerDonante(
      @Parameter(description = "ID de la persona donante") @PathVariable UUID id
  ) {
    return ResponseEntity.ok(personaDonanteService.obtenerDonante(id));
  }

  @Operation(
      summary = "Registrar una persona donante",
      description = "Registra una persona donante humana o jurídica. El email es la clave de idempotencia.",
      responses = {
          @ApiResponse(responseCode = "201", description = "Donante registrado"),
          @ApiResponse(responseCode = "400", description = "Email inválido o datos incompletos"),
          @ApiResponse(responseCode = "409", description = "El email ya está registrado")
      }
  )
  @PostMapping
  public ResponseEntity<Void> registrarDonante(
      @RequestBody @Valid PersonaDonanteRequestDTO dto
  ) {
    UUID id = personaDonanteService.registrar(dto);
    URI location = URI.create("/donantes/" + id);
    return ResponseEntity.created(location).build();
  }

  @Operation(
      summary = "Actualizar una persona donante",
      responses = {
          @ApiResponse(responseCode = "200", description = "Donante actualizado",
              content = @Content(schema = @Schema(implementation = PersonaDonanteResponseDTO.class))),
          @ApiResponse(responseCode = "404", description = "Donante no encontrado")
      }
  )
  @PutMapping("/{id}")
  public ResponseEntity<PersonaDonanteResponseDTO> actualizarDonante(
      @Parameter(description = "ID de la persona donante") @PathVariable UUID id,
      @RequestBody @Valid PersonaDonanteUpdateRequestDTO dto
  ) {
    return ResponseEntity.ok(personaDonanteService.actualizar(id, dto));
  }

  @Operation(
      summary = "Cambiar el estado de una persona donante",
      description = "Transiciona el estado del donante (ACTIVO, INACTIVO, BLOQUEADO), validando que la transición sea legal. Requiere justificación para bloquear.",
      responses = {
          @ApiResponse(responseCode = "204", description = "Estado actualizado"),
          @ApiResponse(responseCode = "400", description = "Falta justificación o mismo estado"),
          @ApiResponse(responseCode = "404", description = "Donante no encontrado"),
          @ApiResponse(responseCode = "422", description = "Transición de estado no permitida")
      }
  )
  @PatchMapping("/{id}/estado")
  public ResponseEntity<Void> cambiarEstado(
      @Parameter(description = "ID de la persona donante") @PathVariable UUID id,
      @RequestBody @Valid EstadoDonanteRequestDTO dto
  ) {
    personaDonanteService.cambiarEstado(id, dto);
    return ResponseEntity.noContent().build();
  }

  @Operation(
      summary = "Modificar un medio de contacto del donante",
      responses = {
          @ApiResponse(responseCode = "204", description = "Contacto actualizado"),
          @ApiResponse(responseCode = "400", description = "Medio de contacto inválido"),
          @ApiResponse(responseCode = "404", description = "Donante no encontrado")
      }
  )
  @PatchMapping("/{id}/contactos")
  public ResponseEntity<Void> modificarContacto(
      @Parameter(description = "ID de la persona donante") @PathVariable UUID id,
      @RequestBody @Valid MedioDeContactoRequestDTO dto
  ) {
    personaDonanteService.modificarContacto(id, dto);
    return ResponseEntity.noContent().build();
  }

  @Operation(
      summary = "Modificar el representante de una persona jurídica",
      responses = {
          @ApiResponse(responseCode = "204", description = "Representante actualizado"),
          @ApiResponse(responseCode = "404", description = "Donante no encontrado"),
          @ApiResponse(responseCode = "400", description = "El donante no es una persona jurídica")
      }
  )
  @PatchMapping("/{id}/representantes")
  public ResponseEntity<Void> modificarRepresentante(
      @Parameter(description = "ID de la persona donante") @PathVariable UUID id,
      @RequestBody @Valid RepresentanteRequestDTO dto
  ) {
    personaDonanteService.modificarRepresentante(id, dto);
    return ResponseEntity.noContent().build();
  }

  @Operation(
      summary = "Eliminar una persona donante",
      responses = {
          @ApiResponse(responseCode = "204", description = "Donante eliminado"),
          @ApiResponse(responseCode = "404", description = "Donante no encontrado")
      }
  )
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminarDonante(
      @Parameter(description = "ID de la persona donante") @PathVariable UUID id
  ) {
    personaDonanteService.eliminar(id);
    return ResponseEntity.noContent().build();
  }

  @Operation(
      summary = "Importar personas donantes desde un archivo CSV",
      description = "Migración masiva de donantes históricos. Si el email ya existe, actualiza los datos; si no, crea el usuario y envía credenciales de acceso.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Reporte de la importación",
              content = @Content(schema = @Schema(implementation = ImportReport.class))),
          @ApiResponse(responseCode = "400", description = "Archivo CSV mal formado")
      }
  )
  @PostMapping("/import")
  public ResponseEntity<ImportReport> importarDonantes(
      @Parameter(description = "Archivo CSV con las personas donantes") @RequestParam("file") MultipartFile file
  ) throws IOException {
    ImportReport report = csvImportService.importar(file.getInputStream());
    return ResponseEntity.ok(report);
  }
}
