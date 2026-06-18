package ar.utn.donatrack.donaciones.repositories;

import ar.utn.donatrack.donaciones.interfaces.repositories.DonacionesRepositoryInterface;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.donacion.EstadoDonacion;
import lombok.Getter;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Repository
@Getter
public class DonacionesRepository implements DonacionesRepositoryInterface {

  /**
   * CORRECCIÓN: la lista original era un ArrayList sin sincronización.
   * PersonaDonanteRepository usa ConcurrentHashMap (thread-safe) pero este repositorio
   * usaba ArrayList, inconsistente y potencialmente corrupto bajo acceso concurrente.
   *
   * Se reemplaza por Collections.synchronizedList para mantener thread-safety
   * con una API idéntica a la original.
   */
  private final List<Donacion> donaciones = Collections.synchronizedList(new ArrayList<>());

  public void cargarDonaciones(List<Donacion> cargaDonaciones) {
    donaciones.addAll(cargaDonaciones);
  }

  public List<Donacion> obtenerTodas() {
    synchronized (donaciones) {
      return new ArrayList<>(donaciones);
    }
  }

  public List<Donacion> obtenerPorEstado(EstadoDonacion estado) {
    synchronized (donaciones) {
      return donaciones.stream()
          .filter(d -> d.getEstado().equals(estado))
          .toList();
    }
  }

  public List<Donacion> obtenerPorDonante(UUID idDonante) {
    synchronized (donaciones) {
      return donaciones.stream()
          .filter(d -> idDonante.equals(d.getIdDonante()))
          .toList();
    }
  }

  public Donacion obtenerPorId(UUID id) {
    synchronized (donaciones) {
      return donaciones.stream()
          .filter(d -> d.getId().equals(id))
          .findFirst()
          .orElse(null);
    }
  }

  public void eliminar(UUID idDonacion) {
    donaciones.removeIf(d -> d.getId().equals(idDonacion));
  }
}