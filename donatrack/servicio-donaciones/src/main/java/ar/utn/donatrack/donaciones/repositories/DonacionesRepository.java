package ar.utn.donatrack.donaciones.repositories;

import ar.utn.donatrack.donaciones.interfaces.repositories.DonacionesRepositoryInterface;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.repositories.jpa.DonacionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Persistencia de donaciones en base relacional (Entrega 4).
 *
 * Antes de esta entrega el almacenamiento era una List sincronizada en memoria.
 * La interfaz DonacionesRepositoryInterface no cambió ni una línea, así que el
 * reemplazo fue invisible para los services y para los 501 tests, que mockean
 * la interfaz y no la implementación. Esa es la razón de ser de la interfaz.
 *
 * Esta clase es un adapter delgado: traduce el vocabulario del dominio
 * ("obtener por estado") al de Spring Data ("findByEstadoNombre").
 */
@Repository
@RequiredArgsConstructor
public class DonacionesRepository implements DonacionesRepositoryInterface {

  private final DonacionJpaRepository jpa;

  @Transactional
  public void cargarDonaciones(List<Donacion> cargaDonaciones) {
    jpa.saveAll(cargaDonaciones);
  }

  @Transactional
  public void guardar(Donacion donacion) {
    jpa.save(donacion);
  }

  @Transactional(readOnly = true)
  public List<Donacion> obtenerTodas() {
    return jpa.findAll();
  }

  @Transactional(readOnly = true)
  public List<Donacion> obtenerPorEstado(String estado) {
    return jpa.findByEstadoNombre(estado);
  }

  @Transactional(readOnly = true)
  public List<Donacion> obtenerPorDonante(UUID idDonante) {
    return jpa.findByIdDonante(idDonante);
  }

  /** Devuelve null y no Optional para no cambiar el contrato que ya usaban los services. */
  @Transactional(readOnly = true)
  public Donacion obtenerPorId(UUID id) {
    return jpa.findById(id).orElse(null);
  }

  @Transactional
  public void eliminar(UUID idDonacion) {
    jpa.deleteById(idDonacion);
  }
}
