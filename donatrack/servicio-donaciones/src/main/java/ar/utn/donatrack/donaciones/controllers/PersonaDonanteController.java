package ar.utn.donatrack.donaciones.controllers;

import ar.utn.donatrack.donaciones.services.PersonaDonanteService;
import ar.utn.donatrack.donaciones.dtos.response.ImportacionResumenResponseDTO;
import ar.utn.donatrack.donaciones.dtos.request.EstadoDonanteRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.MedioDeContactoRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.PersonaDonanteRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.PersonaDonanteUpdateRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.RepresentanteRequestDTO;
import ar.utn.donatrack.donaciones.dtos.response.PaginaResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.PersonaDonanteResponseDTO;
import ar.utn.donatrack.donaciones.services.CsvImportService;
import ar.utn.donatrack.donaciones.util.SolicitudPagina;
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
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/donantes")
@Validated
public class PersonaDonanteController {

  private final PersonaDonanteService personaDonanteService;
  private final CsvImportService csvImportService;

  /**
   * Listado paginado y ordenado por email (Entrega 5). Páginas desde 0, 20
   * elementos por defecto y como máximo 100; fuera de rango responde 400.
   */
  @GetMapping
  public ResponseEntity<PaginaResponseDTO<PersonaDonanteResponseDTO>> obtenerDonantes(
      @RequestParam(required = false) String estado,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size
  ) {
    return ResponseEntity.ok(personaDonanteService.obtenerDonantes(estado, SolicitudPagina.de(page, size)));
  }

  @GetMapping("/{id}")
  public ResponseEntity<PersonaDonanteResponseDTO> obtenerDonante(
      @PathVariable UUID id
  ) {
    return ResponseEntity.ok(personaDonanteService.obtenerDonante(id));
  }

  @PostMapping
  public ResponseEntity<PersonaDonanteResponseDTO> registrarDonante(
      @RequestBody @Valid PersonaDonanteRequestDTO dto
  ) {
    PersonaDonanteResponseDTO donanteCreado = personaDonanteService.registrar(dto);
    URI location = URI.create("/donantes/" + donanteCreado.getId());
    return ResponseEntity.created(location).body(donanteCreado);
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

  /**
   * Importación masiva por CSV. Responde 200 aunque haya filas con error (el
   * archivo se procesó) con un resumen y el detalle de esas filas; si el archivo
   * supera el límite de subida responde 413.
   */
  @PostMapping("/import")
  public ResponseEntity<ImportacionResumenResponseDTO> importarDonantes(
      @RequestParam("file") MultipartFile file
  ) throws IOException {
    return ResponseEntity.ok(csvImportService.importarYResumir(file.getInputStream()));
  }
}
