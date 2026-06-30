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
public class DonacionesController {

  private final DonacionServiceInterface donacionService;
  private final SegmentadorDonacionesServiceInterface segmentadorDonacionesService;
  private final PersonasValidator personasValidator;
  private final DonacionMapper mapper;

  @GetMapping
  public ResponseEntity<List<DonacionResponseDTO>> obtenerDonaciones(
      @RequestParam(required = false) String estado,
      @RequestParam(required = false) UUID idDonante,
      @RequestParam(required = false) String subcategoria
  ) {
    return ResponseEntity.ok(donacionService.obtenerDonaciones(estado, idDonante, subcategoria));
  }

  @GetMapping("/{id}")
  public ResponseEntity<DonacionResponseDTO> obtenerDonacion(
      @PathVariable UUID id
  ) {
    return ResponseEntity.ok(donacionService.obtenerPorId(id));
  }

  @PostMapping
  public ResponseEntity<List<DonacionResponseDTO>> registrar(
      @RequestBody @Valid DonacionRequestDTO dto
  ) {
    personasValidator.validarExistenciaPersona(dto.getIdDonante());
    CargaDonacion carga = mapper.toCargaDonacion(dto);
    List<Donacion> donacionesCreadas = segmentadorDonacionesService.segmentar(carga);
    return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toDTOList(donacionesCreadas));
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

  @GetMapping("/{id}/candidatos")
  public ResponseEntity<CandidatosAsignacionResponseDTO> obtenerCandidatos(
      @PathVariable UUID id
  ) {
    return ResponseEntity.ok(donacionService.obtenerCandidatos(id));
  }

  @PatchMapping("/{id}/asignar")
  public ResponseEntity<Void> asignarEntidad(
      @PathVariable UUID id,
      @RequestBody @Valid AsignacionRequestDTO dto
  ) {
    donacionService.asignar(id, dto);
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