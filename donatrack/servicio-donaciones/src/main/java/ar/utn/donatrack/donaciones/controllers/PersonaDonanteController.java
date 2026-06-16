package ar.utn.donatrack.donaciones.controllers;

import ar.utn.donatrack.donaciones.dtos.request.EstadoUpdateDTO;
import ar.utn.donatrack.donaciones.dtos.request.MedioDeContactoDTO;
import ar.utn.donatrack.donaciones.dtos.request.PersonaDonanteRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.RepresentanteDTO;
import ar.utn.donatrack.donaciones.dtos.response.PersonaDonanteResponseDTO;
import ar.utn.donatrack.donaciones.models.contacto.MedioDeContacto;
import ar.utn.donatrack.donaciones.models.donante.EstadoDonante;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.donante.Representante;
import ar.utn.donatrack.donaciones.services.PersonaDonanteService;
import ar.utn.donatrack.donaciones.validations.PersonasValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/donantes")
public class PersonaDonanteController {

  private final PersonaDonanteService personaDonanteService;

  @GetMapping
  public ResponseEntity<List<PersonaDonanteResponseDTO>> obtenerDonantes(
      @RequestParam(required = false) EstadoDonante estado) {

    return ResponseEntity.ok(personaDonanteService.obtenerDonantes(estado));
  }

  @GetMapping("/{id}")
  public ResponseEntity<PersonaDonanteResponseDTO> obtenerDonante(@PathVariable UUID id) {
    return ResponseEntity.ok(personaDonanteService.obtenerDonante(id));
  }

  @PostMapping
  public ResponseEntity<Void> registrarDonante(@RequestBody @Valid PersonaDonanteRequestDTO dto) {
    UUID id = personaDonanteService.registrar(dto);
    URI location = URI.create("/donantes/" + id);
    return ResponseEntity.created(location).build();
  }

  @PatchMapping("/{id}/estado")
  public ResponseEntity<Void> cambiarEstado(
      @PathVariable UUID id,
      @RequestBody @Valid EstadoUpdateDTO dto) {

    personaDonanteService.cambiarEstado(id, dto);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{id}/contactos")
  public ResponseEntity<Void> modificarContacto(
      @PathVariable UUID id,
      @RequestBody @Valid MedioDeContactoDTO dto) {

    personaDonanteService.modificarContacto(id, dto);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{id}/representantes")
  public ResponseEntity<Void> modificarRepresentante(
      @PathVariable UUID id,
      @RequestBody @Valid RepresentanteDTO dto) {

    personaDonanteService.modificarRepresentante(id, dto);
    return ResponseEntity.noContent().build();
  }
}