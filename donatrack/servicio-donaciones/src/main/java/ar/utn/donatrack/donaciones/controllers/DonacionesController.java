package ar.utn.donatrack.donaciones.controllers;


import ar.utn.donatrack.donaciones.models.donacion.bien.Bien;
import ar.utn.donatrack.donaciones.services.SegmentadorDonacionesService;
import ar.utn.donatrack.donaciones.validations.PersonasValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
public class DonacionesController {

  private final SegmentadorDonacionesService segmentadorDonacionesService;
  private final PersonasValidator personasValidator;

  @PostMapping
  public ResponseEntity<Void> segmentar(
      @RequestBody List<Bien> bienesASegmentar, // 👈 @RequestBody indica que esto viene en el JSON de la petición
      @RequestParam UUID idDonante              // 👈 @RequestParam si viene en la URL como ?idDonante=...
  ) {
    personasValidator.validarExistenciaPersona(idDonante);

    segmentadorDonacionesService.segmentar(bienesASegmentar, idDonante);

    return ResponseEntity.ok().build();
  }
}
