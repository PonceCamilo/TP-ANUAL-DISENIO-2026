package ar.utn.donatrack.donaciones.repositories;

import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.ListJoin;
import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienPerecible;
import ar.utn.donatrack.donaciones.models.donacion.bien.Bien;
import ar.utn.donatrack.donaciones.interfaces.repositories.DonacionesRepositoryInterface;
import ar.utn.donatrack.donaciones.interfaces.repositories.FiltroDonaciones;
import ar.utn.donatrack.donaciones.models.asignacion.RecepcionDiariaEntidad;
import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.repositories.jpa.DonacionJpaRepository;
import ar.utn.donatrack.donaciones.repositories.jpa.RecepcionDiariaJpaRepository;
import ar.utn.donatrack.donaciones.util.Pagina;
import ar.utn.donatrack.donaciones.util.SolicitudPagina;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
  private final RecepcionDiariaJpaRepository recepciones;

  @Transactional
  public void cargarDonaciones(List<Donacion> cargaDonaciones) {
    jpa.saveAll(cargaDonaciones);
  }

  @Transactional
  public void guardar(Donacion donacion) {
    jpa.save(donacion);
  }

  /**
   * La desnormalización es un detalle de persistencia: el dominio solo asigna,
   * y este adapter mueve el contador de la entidad anterior (si la había) a la
   * nueva, todo en una transacción.
   */
  @Transactional
  public void guardarAsignacion(Donacion donacion) {
    Optional<DonacionJpaRepository.AsignacionActual> previa = jpa.buscarAsignacion(donacion.getId());
    jpa.save(donacion);

    UUID entidadPrevia = previa.map(DonacionJpaRepository.AsignacionActual::getIdEntidadBeneficiaria).orElse(null);
    LocalDate fechaPrevia = previa.map(DonacionJpaRepository.AsignacionActual::getFechaAsignacion).orElse(null);
    if (Objects.equals(entidadPrevia, donacion.getIdEntidadBeneficiaria())
        && Objects.equals(fechaPrevia, donacion.getFechaAsignacion())) {
      return;
    }
    ajustarRecepciones(entidadPrevia, fechaPrevia, -1);
    ajustarRecepciones(donacion.getIdEntidadBeneficiaria(), donacion.getFechaAsignacion(), 1);
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

  @Transactional(readOnly = true)
  public boolean existenDelDonante(UUID idDonante) {
    return jpa.existsByIdDonante(idDonante);
  }

  @Transactional(readOnly = true)
  public boolean existenAsignadasA(UUID idEntidad) {
    return jpa.existsByIdEntidadBeneficiaria(idEntidad);
  }

  @Transactional
  public void eliminar(UUID idDonacion) {
    Optional<DonacionJpaRepository.AsignacionActual> previa = jpa.buscarAsignacion(idDonacion);
    jpa.deleteById(idDonacion);
    previa.ifPresent(asignacion ->
        ajustarRecepciones(asignacion.getIdEntidadBeneficiaria(), asignacion.getFechaAsignacion(), -1));
  }

  /**
   * Suma o resta una recepción. Si la fila del día todavía no existe y hay que
   * sumar, la crea. Si hubiera que restar de una fila inexistente (un desfase),
   * no hace nada: lo corrige la reconciliación nocturna.
   */
  private void ajustarRecepciones(UUID idEntidad, LocalDate fecha, long delta) {
    if (idEntidad == null || fecha == null) {
      return;
    }
    if (recepciones.sumar(idEntidad, fecha, delta) == 0 && delta > 0) {
      recepciones.save(new RecepcionDiariaEntidad(idEntidad, fecha, delta));
    }
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
    if (filtro.soloConBienesVencidos()) {
      LocalDate hoy = FechaHoraArgentina.hoy();
      spec = spec.and((raiz, consulta, cb) -> {
        Subquery<UUID> vencidos = consulta.subquery(UUID.class);
        Root<Donacion> donacion = vencidos.from(Donacion.class);
        ListJoin<Donacion, Bien> bien = donacion.joinList("bienes");
        vencidos.select(donacion.get("id")).where(
            cb.equal(donacion.get("id"), raiz.get("id")),
            cb.lessThan(cb.treat(bien, BienPerecible.class).get("fechaVencimiento"), hoy));
        return cb.exists(vencidos);
      });
    }
    return spec;
  }
}
