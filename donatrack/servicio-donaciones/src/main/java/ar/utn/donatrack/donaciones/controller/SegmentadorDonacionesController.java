package ar.utn.donatrack.donaciones.controller;

import ar.utn.donatrack.donaciones.services.SegmentadorDonacionesService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SegmentadorDonacionesController {

  private final SegmentadorDonacionesService segmentadorDonacionesService;

  public void cargarDonacion(){
  }
}
