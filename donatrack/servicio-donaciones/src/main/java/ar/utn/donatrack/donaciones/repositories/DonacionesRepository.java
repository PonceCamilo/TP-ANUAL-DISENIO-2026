package ar.utn.donatrack.donaciones.repositories;

import ar.utn.donatrack.donaciones.interfaces.repositories.DonacionesRepositoryInterface;
import ar.utn.donatrack.donaciones.models.donacion.CambioEstado;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.donacion.EstadoDonacion;
import ar.utn.donatrack.donaciones.models.donacion.bien.Bien;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Repository;

@Repository
@Getter
@Setter
public class DonacionesRepository implements DonacionesRepositoryInterface {

  private List<Donacion> donaciones = new ArrayList<>();

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
  public void cambiarEstado(UUID idDonacion, EstadoDonacion nuevoEstado, String justificacion) {
    Donacion donacion = obtenerPorId(idDonacion);
    if (donacion != null) {
      donacion.setEstado(nuevoEstado);
      donacion.getHistorialEstados().add(CambioEstado.builder()
          .estado(nuevoEstado)
          .justificacion(justificacion)
          .build());
    }
  }

  @Override
  public void modificarBien(UUID idDonacion, Bien bien) {
    Donacion donacion = obtenerPorId(idDonacion);
    if (donacion != null && !donacion.getBienes().isEmpty()) {
      donacion.getBienes().set(0, bien);
    }
  }

  @Override
  public void eliminar(UUID idDonacion) {
    donaciones.removeIf(d -> d.getId().equals(idDonacion));
  }
}
