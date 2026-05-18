package ar.utn.donatrack.donaciones.repositories;

import ar.utn.donatrack.donaciones.interfaces.repositories.SegmentadorDonacionesRepositoryInterface;
import ar.utn.donatrack.donaciones.model.donacion.Donacion;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SegmentadorDonacionesRepository implements SegmentadorDonacionesRepositoryInterface {

  private static SegmentadorDonacionesRepository instance = null;
  private SegmentadorDonacionesRepository(List<Donacion> donaciones) {
    this.donaciones = donaciones;
  }

  public static SegmentadorDonacionesRepository instance() {
    if (instance == null) {
      instance = new SegmentadorDonacionesRepository(new ArrayList<>());
    }
    return instance;
  }

  private final List<Donacion> donaciones;

  public void cargarDonaciones(List<Donacion> cargaDonacion) {
    donaciones.addAll(cargaDonacion);
  }
}