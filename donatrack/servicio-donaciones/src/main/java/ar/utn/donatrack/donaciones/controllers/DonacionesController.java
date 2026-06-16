package ar.utn.donatrack.donaciones.controllers;

import ar.utn.donatrack.donaciones.dtos.response.DonacionResponseDTO;
import ar.utn.donatrack.donaciones.mappers.DonacionMapper;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.donacion.EstadoDonacion;
import ar.utn.donatrack.donaciones.models.donacion.bien.Bien;
import ar.utn.donatrack.donaciones.services.SegmentadorDonacionesService;
import ar.utn.donatrack.donaciones.validations.PersonasValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
import ar.utn.donatrack.donaciones.dtos.request.CambioEstadoRequestDTO;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/donaciones")
public class DonacionesController {

  private final SegmentadorDonacionesService segmentadorDonacionesService;
  private final PersonasValidator personasValidator;

  @GetMapping
  public ResponseEntity<List<DonacionResponseDTO>> obtenerDonaciones(
      @RequestParam(value = "estado", required = false) EstadoDonacion estado
  ) {
    List<Donacion> donaciones;

    if (estado != null) {
      donaciones = segmentadorDonacionesService.obtenerDonacionesPorEstado(estado);
    } else {
      donaciones = segmentadorDonacionesService.obtenerTodasLasDonaciones();
    }

    return ResponseEntity.ok(DonacionMapper.toDTOList(donaciones));
  }

  @GetMapping("/{id}")
  public ResponseEntity<DonacionResponseDTO> obtenerDonacion(
      @PathVariable UUID id
  ) {
    Donacion donacion = segmentadorDonacionesService.obtenerDonacionPorId(id);

    return ResponseEntity.ok(DonacionMapper.toDTO(donacion));
  }

  @PostMapping
  public ResponseEntity<Void> segmentar(
      @RequestBody List<Bien> bienesASegmentar,
      @RequestParam UUID idDonante
  ) {
    personasValidator.validarExistenciaPersona(idDonante);

    segmentadorDonacionesService.segmentar(bienesASegmentar, idDonante);

    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @PatchMapping("/{id}")
  public ResponseEntity<Void> cambiarEstadoDonacion(
      @PathVariable UUID id,
      @RequestBody CambioEstadoRequestDTO cambioEstado
  ) {
    segmentadorDonacionesService.cambiarEstadoDonacion(id, cambioEstado.getEstado(), cambioEstado.getJustificacion());
    return ResponseEntity.ok().build();
  }

  @PatchMapping("/{id}/bien")
  public ResponseEntity<Void> modificarBienDonacion(
      @PathVariable UUID id,
      @RequestBody Bien bien
  ) {
    segmentadorDonacionesService.modificarBien(id, bien);

    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminarDonacion(
      @PathVariable UUID id
  ) {
    segmentadorDonacionesService.eliminar(id);
    return ResponseEntity.noContent().build();
  }
}