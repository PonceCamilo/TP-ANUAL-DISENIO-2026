package ar.utn.donatrack.donaciones.repositories;

import ar.utn.donatrack.donaciones.interfaces.repositories.DonacionesRepositoryInterface;
import ar.utn.donatrack.donaciones.interfaces.repositories.FiltroDonaciones;
import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.repositories.jpa.DonacionJpaRepository;
import ar.utn.donatrack.donaciones.util.Pagina;
import ar.utn.donatrack.donaciones.util.SolicitudPagina;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Persistencia de donaciones en base relacional (Entrega 4).
 *
 * Antes de la Entrega 4 el almacenamiento era una List sincronizada en memoria.
 * La interfaz DonacionesRepositoryInterface permitió reemplazarlo sin que los
 * services ni sus tests, que mockean la interfaz, se enteraran.
 *
 * Esta clase es un adapter delgado: traduce el vocabulario del dominio
 * ("obtener por estado") al de Spring Data ("findByEstadoNombre").
 */
@Repository
@RequiredArgsConstructor
public class DonacionesRepository implements DonacionesRepositoryInterface {

  /** Más nuevas primero; el id desempata para que la paginación sea estable. */
  private static final Sort ORDEN_LISTADO = Sort.by(Sort.Order.desc("fechaDonacion"), Sort.Order.asc("id"));

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
  public Pagina<Donacion> buscar(FiltroDonaciones filtro, SolicitudPagina pagina) {
    Page<Donacion> resultado = jpa.findAll(
        especificacion(filtro),
        PageRequest.of(pagina.numero(), pagina.tamanio(), ORDEN_LISTADO));
    return new Pagina<>(resultado.getContent(), pagina.numero(), pagina.tamanio(), resultado.getTotalElements());
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

  /**
   * Cada filtro presente agrega una condición; los ausentes no restringen.
   * La subcategoría se compara contra la columna normalizada usando la misma
   * normalización que Subcategoria, así "Arroz" y " arroz " son la misma.
   */
  private static Specification<Donacion> especificacion(FiltroDonaciones filtro) {
    Specification<Donacion> spec = (raiz, consulta, cb) -> cb.conjunction();
    if (filtro.estado() != null && !filtro.estado().isBlank()) {
      spec = spec.and((raiz, consulta, cb) -> cb.equal(raiz.get("estadoNombre"), filtro.estado()));
    }
    if (filtro.idDonante() != null) {
      spec = spec.and((raiz, consulta, cb) -> cb.equal(raiz.get("idDonante"), filtro.idDonante()));
    }
    if (filtro.idEntidad() != null) {
      spec = spec.and((raiz, consulta, cb) -> cb.equal(raiz.get("idEntidadBeneficiaria"), filtro.idEntidad()));
    }
    if (filtro.subcategoria() != null && !filtro.subcategoria().isBlank()) {
      String normalizada = new Subcategoria(filtro.subcategoria()).getTipoNormalizado();
      spec = spec.and((raiz, consulta, cb) ->
          cb.equal(raiz.get("subcategoria").get("tipoNormalizado"), normalizada));
    }
    return spec;
  }
}
