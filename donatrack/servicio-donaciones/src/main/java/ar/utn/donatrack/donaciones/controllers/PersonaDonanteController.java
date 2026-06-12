package ar.utn.donatrack.donaciones.controllers;

import ar.utn.donatrack.donaciones.models.contacto.MedioDeContacto;
import ar.utn.donatrack.donaciones.models.donante.EstadoDonante;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.donante.Representante;
import ar.utn.donatrack.donaciones.services.PersonaDonanteService;
import ar.utn.donatrack.donaciones.validations.PersonasValidator;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/donantes")
public class PersonaDonanteController {

  private final PersonaDonanteService personaDonanteService;
  private final PersonasValidator personasValidator;

  @GetMapping
  public ResponseEntity<List<PersonaDonante>> obtenerDonantes(
      @RequestParam(value = "estado", required = false) EstadoDonante estado) {

    List<PersonaDonante> donantes;

    if (estado != null) {
      donantes = personaDonanteService.obtenerDonantesPorEstado(estado);
    } else {
      donantes = personaDonanteService.obtenerPersonasDonantes();
    }

    return ResponseEntity.ok(donantes);
  }

  // haciendo un GET a /donantes?estado=ACTIVO o /donantes?estado=INACTIVO,
  // consigo todos los donantes que tengan ese estado

  @GetMapping("/{id}")
  public ResponseEntity<PersonaDonante> obtenerDonante(
      @PathVariable UUID id
  ) {
    personasValidator.validarExistenciaPersona(id, null);

    PersonaDonante donante = personaDonanteService.obtenerPersona(id, null);

    return ResponseEntity.ok(donante);
  }

  @PostMapping
  public ResponseEntity<Void> registrarDonante(
      @RequestBody PersonaDonante donante
  ) {
    personasValidator.validarExistenciaMail(donante.getEmail());

    personaDonanteService.registrar(donante);

    return ResponseEntity.ok().build();
  }

  @PatchMapping("/{id}")
  public ResponseEntity<Void> cambiarEstadoDonante(
      @PathVariable UUID id,
      @RequestBody EstadoDonante estado
  ) {
    personasValidator.validarExistenciaPersona(id, null);

    personaDonanteService.cambiarEstadoPersona(id, estado);

    return ResponseEntity.ok().build();
  }

  @PatchMapping("/{id}/contactos")
  public ResponseEntity<Void> modificarMedioContacto(
      @PathVariable UUID id,
      @RequestBody MedioDeContacto medioContacto
  ) {
    personasValidator.validarExistenciaPersona(id, null);

    personaDonanteService.modificarMedioContacto(id, medioContacto);

    return ResponseEntity.ok().build();
  }

  @PatchMapping("/{id}/representantes")
  public ResponseEntity<Void> modificarRepresentante(
      @PathVariable UUID id,
      @RequestBody Representante representante
  ) {
    personasValidator.validarExistenciaPersona(id, null);

    personaDonanteService.modificarRepresentante(id, representante);

    return ResponseEntity.ok().build();
  }
}
