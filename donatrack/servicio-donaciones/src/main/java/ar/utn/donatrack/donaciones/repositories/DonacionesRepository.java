package ar.utn.donatrack.donaciones.repositories;

import ar.utn.donatrack.donaciones.interfaces.repositories.DonacionesRepositoryInterface;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.donacion.EstadoDonacion;
import lombok.Getter;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
@Getter
public class DonacionesRepository implements DonacionesRepositoryInterface {

  private final List<Donacion> donaciones = new ArrayList<>();

  @Override
  public void cargarDonaciones(List<Donacion> cargaDonaciones) {
    donaciones.addAll(cargaDonaciones);
  }

  @Override
  public List<Donacion> obtenerTodas() {
    return new ArrayList<>(donaciones);
  }

  @Override
  public List<Donacion> obtenerPorEstado(EstadoDonacion estado) {
    return donaciones.stream()
        .filter(d -> d.getEstado().equals(estado))
        .toList();
  }

  @Override
  public Donacion obtenerPorId(UUID id) {
    return donaciones.stream()
        .filter(d -> d.getId().equals(id))
        .findFirst()
        .orElse(null);
  }

  @Override
  public void eliminar(UUID idDonacion) {
    donaciones.removeIf(d -> d.getId().equals(idDonacion));
  }
}
