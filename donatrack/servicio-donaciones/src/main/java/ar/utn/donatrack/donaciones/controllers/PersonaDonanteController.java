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
public class PersonaDonanteController {

  private final PersonaDonanteServiceInterface personaDonanteService;
  private final CsvImportService csvImportService;

  @GetMapping
  public ResponseEntity<List<PersonaDonanteResponseDTO>> obtenerDonantes(
      @RequestParam(required = false) EstadoDonante estado
  ) {
    return ResponseEntity.ok(personaDonanteService.obtenerDonantes(estado));
  }

  @GetMapping("/{id}")
  public ResponseEntity<PersonaDonanteResponseDTO> obtenerDonante(
      @PathVariable UUID id
  ) {
    return ResponseEntity.ok(personaDonanteService.obtenerDonante(id));
  }

  @PostMapping
  public ResponseEntity<Void> registrarDonante(
      @RequestBody @Valid PersonaDonanteRequestDTO dto
  ) {
    UUID id = personaDonanteService.registrar(dto);
    URI location = URI.create("/donantes/" + id);
    return ResponseEntity.created(location).build();
  }

  @PutMapping("/{id}")
  public ResponseEntity<PersonaDonanteResponseDTO> actualizarDonante(
      @PathVariable UUID id,
      @RequestBody @Valid PersonaDonanteUpdateRequestDTO dto
  ) {
    return ResponseEntity.ok(personaDonanteService.actualizar(id, dto));
  }

  @PatchMapping("/{id}/estado")
  public ResponseEntity<Void> cambiarEstado(
      @PathVariable UUID id,
      @RequestBody @Valid EstadoDonanteRequestDTO dto
  ) {
    personaDonanteService.cambiarEstado(id, dto);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{id}/contactos")
  public ResponseEntity<Void> modificarContacto(
      @PathVariable UUID id,
      @RequestBody @Valid MedioDeContactoRequestDTO dto
  ) {
    personaDonanteService.modificarContacto(id, dto);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{id}/representantes")
  public ResponseEntity<Void> modificarRepresentante(
      @PathVariable UUID id,
      @RequestBody @Valid RepresentanteRequestDTO dto
  ) {
    personaDonanteService.modificarRepresentante(id, dto);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminarDonante(
      @PathVariable UUID id
  ) {
    personaDonanteService.eliminar(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/import")
  public ResponseEntity<ImportReport> importarDonantes(
      @RequestParam("file") MultipartFile file
  ) throws IOException {
    ImportReport report = csvImportService.importar(file.getInputStream());
    return ResponseEntity.ok(report);
  }
}
