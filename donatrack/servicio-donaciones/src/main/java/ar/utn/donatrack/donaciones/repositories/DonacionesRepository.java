package ar.utn.donatrack.donaciones.repositories;

import ar.utn.donatrack.donaciones.interfaces.repositories.DonacionesRepositoryInterface;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Repository;

@Repository
@Getter
@Setter
public class DonacionesRepository implements DonacionesRepositoryInterface {

  private List<Donacion> donaciones = new ArrayList<>();

  public void cargarDonaciones(List<Donacion> cargaDonaciones) {
    donaciones.addAll(cargaDonaciones);
  }
}