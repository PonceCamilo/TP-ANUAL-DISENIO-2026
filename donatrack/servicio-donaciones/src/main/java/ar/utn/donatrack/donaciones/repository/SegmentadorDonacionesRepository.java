package ar.utn.donatrack.donaciones.repository;

import ar.utn.donatrack.donaciones.model.donacion.Donacion;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SegmentadorDonacionesRepository {

  private static SegmentadorDonacionesRepository _instance;

  public static SegmentadorDonacionesRepository instance() {
    if (_instance == null) {
      _instance = new SegmentadorDonacionesRepository(new ArrayList<>());
    }
    return _instance;
  }

  private final List<Donacion> donaciones;

  public SegmentadorDonacionesRepository(List<Donacion> donaciones) {
    this.donaciones = donaciones;
  }

  public void cargarDonaciones(List<Donacion> cargaDonacion) {
    donaciones.addAll(cargaDonacion);
  }
}
