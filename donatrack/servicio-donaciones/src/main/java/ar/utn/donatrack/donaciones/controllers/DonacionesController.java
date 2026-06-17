package ar.utn.donatrack.donaciones.controllers;

import ar.utn.donatrack.donaciones.dtos.request.BienRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.CambioEstadoRequestDTO;
import ar.utn.donatrack.donaciones.dtos.response.DonacionResponseDTO;
import ar.utn.donatrack.donaciones.interfaces.services.DonacionServiceInterface;
import ar.utn.donatrack.donaciones.interfaces.services.SegmentadorDonacionesServiceInterface;
import ar.utn.donatrack.donaciones.models.donacion.EstadoDonacion;
import ar.utn.donatrack.donaciones.validations.PersonasValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.ResponseEntity;
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
public class DonacionesController {

  private final DonacionServiceInterface donacionService;
  private final SegmentadorDonacionesServiceInterface segmentadorDonacionesService;
  private final PersonasValidator personasValidator;

  @GetMapping
  public ResponseEntity<List<DonacionResponseDTO>> obtenerDonaciones(
      @RequestParam(required = false) EstadoDonacion estado
  ) {
    return ResponseEntity.ok(donacionService.obtenerDonaciones(estado));
  }

  @GetMapping("/{id}")
  public ResponseEntity<DonacionResponseDTO> obtenerDonacion(
      @PathVariable UUID id
  ) {
    return ResponseEntity.ok(donacionService.obtenerPorId(id));
  }

  @PostMapping
  public ResponseEntity<Void> segmentar(
      @RequestBody @Valid List<BienRequestDTO> bienes,
      @RequestParam UUID idDonante
  ) {
    personasValidator.validarExistenciaPersona(idDonante);

    segmentadorDonacionesService.segmentar(bienes, idDonante);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @PatchMapping("/{id}/estado")
  public ResponseEntity<Void> cambiarEstadoDonacion(
      @PathVariable UUID id,
      @RequestBody @Valid CambioEstadoRequestDTO dto
  ) {
    donacionService.cambiarEstado(id, dto);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{id}/bien")
  public ResponseEntity<Void> modificarBienDonacion(
      @PathVariable UUID id,
      @RequestBody @Valid BienRequestDTO dto
  ) {
    donacionService.modificarBien(id, dto);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminarDonacion(
      @PathVariable UUID id
  ) {
    donacionService.eliminar(id);
    return ResponseEntity.noContent().build();
  }
}
