package ar.utn.donatrack.donaciones.repositories;

import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.ListJoin;
import ar.utn.donatrack.donaciones.models.donacion.Asignacion;
import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienPerecible;
import ar.utn.donatrack.donaciones.models.donacion.bien.Bien;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioDonaciones;
import ar.utn.donatrack.donaciones.util.FiltroDonaciones;
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

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistencia de donaciones en base relacional (Entrega 4).
 *
 * Antes de la Entrega 4 el almacenamiento era una List sincronizada en memoria.
 * La interfaz RepositorioDonaciones permitió reemplazarlo sin que los
 * services ni sus tests, que mockean la interfaz, se enteraran.
 *
 * Esta clase es un adapter delgado: traduce el vocabulario del dominio
 * ("obtener un lote en estado") al de Spring Data ("findByEstadoNombre").
 */
@Repository
@RequiredArgsConstructor
public class DonacionesRepository implements RepositorioDonaciones {

  /** Más nuevas primero; el id desempata para que la paginación sea estable. */
  private static final Sort ORDEN_LISTADO = Sort.by(Sort.Order.desc("fechaDonacion"), Sort.Order.asc("id"));

  private final DonacionJpaRepository jpa;
  private final RecepcionDiariaJpaRepository recepciones;

  public void cargarDonaciones(List<Donacion> cargaDonaciones) {
    jpa.saveAll(cargaDonaciones);
  }

  /**
   * Un solo guardado para todo: si la asignación cambió, mueve el contador
   * desnormalizado de recepciones (resta una a la asignación anterior, suma una
   * a la nueva) en la misma transacción. La asignación anterior la recuerda la
   * donación, así que no hace falta leerla de la base; sin cambio de asignación,
   * no hay trabajo extra. Una baja lógica cuenta como quitar la asignación: la
   * recepción se descuenta.
   */
  public void guardar(Donacion donacion) {
    if (donacion.cambioLaAsignacion()) {
      ajustarRecepciones(donacion.asignacionGuardada(), -1);
      ajustarRecepciones(donacion.asignacion(), 1);
    }
    jpa.save(donacion);
    donacion.marcarAsignacionGuardada();
  }

  public List<Donacion> obtenerLoteEnEstado(String estado, UUID despuesDe, int cantidad) {
    PageRequest lote = PageRequest.of(0, cantidad, Sort.by("id"));
    return despuesDe == null
        ? jpa.findByEstadoNombre(estado, lote)
        : jpa.findByEstadoNombreAndIdGreaterThan(estado, despuesDe, lote);
  }

  public Pagina<Donacion> buscar(FiltroDonaciones filtro, SolicitudPagina pagina) {
    Page<Donacion> resultado = jpa.findAll(
        especificacion(filtro),
        PageRequest.of(pagina.numero(), pagina.tamanio(), ORDEN_LISTADO));
    return new Pagina<>(resultado.getContent(), pagina.numero(), pagina.tamanio(), resultado.getTotalElements());
  }

  public Optional<Donacion> buscarPorId(UUID id) {
    return jpa.findById(id);
  }

  public List<Donacion> obtenerPorIds(Collection<UUID> ids) {
    return ids.isEmpty() ? List.of() : jpa.findAllById(ids);
  }

  public boolean existenDelDonante(UUID idDonante) {
    return jpa.existsByIdDonante(idDonante);
  }

  public boolean existenAsignadasA(UUID idEntidad) {
    return jpa.existsByIdEntidadBeneficiaria(idEntidad);
  }


  /**
   * Suma o resta una recepción. Si la fila del día todavía no existe y hay que
   * sumar, la crea. Si hubiera que restar de una fila inexistente (un desfase),
   * no hace nada: lo corrige la reconciliación nocturna.
   */
  private void ajustarRecepciones(Asignacion asignacion, long delta) {
    if (!asignacion.existe()) {
      return;
    }
    if (recepciones.sumar(asignacion.idEntidad(), asignacion.fecha(), delta) == 0 && delta > 0) {
      recepciones.save(new RecepcionDiariaEntidad(asignacion.idEntidad(), asignacion.fecha(), delta));
    }
  }

  /**
   * Una condición por filtro presente; Specification.allOf ignora las nulas, que
   * son los filtros que no vinieron. Sumar un filtro es una línea más acá.
   */
  private static Specification<Donacion> especificacion(FiltroDonaciones filtro) {
    return Specification.allOf(
        igual("estadoNombre", filtro.estado()),
        igual("idDonante", filtro.idDonante()),
        igual("idEntidadBeneficiaria", filtro.idEntidad()),
        deSubcategoria(filtro.subcategoria()),
        filtro.soloConBienesVencidos() ? conBienesVencidosAl(FechaHoraArgentina.hoy()) : null);
  }

  /** El atributo es igual al valor, o ninguna condición (null) si el filtro no vino. */
  private static Specification<Donacion> igual(String atributo, Object valor) {
    return valor == null ? null : (raiz, consulta, cb) -> cb.equal(raiz.get(atributo), valor);
  }

  /**
   * Se compara contra la columna normalizada con la misma normalización que
   * Subcategoria, así "Arroz" y " arroz " son la misma.
   */
  private static Specification<Donacion> deSubcategoria(String subcategoria) {
    if (subcategoria == null) {
      return null;
    }
    String normalizada = new Subcategoria(subcategoria).getTipoNormalizado();
    return (raiz, consulta, cb) -> cb.equal(raiz.get("subcategoria").get("tipoNormalizado"), normalizada);
  }

  /** Donaciones con algún bien perecedero vencido a la fecha. */
  private static Specification<Donacion> conBienesVencidosAl(LocalDate fecha) {
    return (raiz, consulta, cb) -> {
      Subquery<UUID> vencidos = consulta.subquery(UUID.class);
      Root<Donacion> donacion = vencidos.from(Donacion.class);
      ListJoin<Donacion, Bien> bien = donacion.joinList("bienes");
      vencidos.select(donacion.get("id")).where(
          cb.equal(donacion.get("id"), raiz.get("id")),
          cb.lessThan(cb.treat(bien, BienPerecible.class).get("fechaVencimiento"), fecha));
      return cb.exists(vencidos);
    };
  }
}
