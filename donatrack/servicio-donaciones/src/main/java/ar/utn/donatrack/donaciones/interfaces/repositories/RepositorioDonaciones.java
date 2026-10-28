package ar.utn.donatrack.donaciones.interfaces.repositories;

import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.util.Pagina;
import ar.utn.donatrack.donaciones.util.SolicitudPagina;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioDonaciones {
  void cargarDonaciones(List<Donacion> donaciones);

  /** Guarda una donación nueva o los cambios de una existente. */
  void guardar(Donacion donacion);

  /**
   * Guarda una donación a la que se le acaba de asignar (o reasignar) una
   * entidad beneficiaria, y en la misma transacción actualiza el contador
   * desnormalizado de donaciones recibidas por entidad (Entrega 5).
   *
   * Que vaya junto es lo que garantiza que el contador no quede desfasado si
   * algo falla a mitad de camino: o se guardan las dos cosas o ninguna.
   */
  void guardarAsignacion(Donacion donacion);

  List<Donacion> obtenerPorEstado(String estado);

  /**
   * Listado paginado con los filtros aplicados en la base (Entrega 5). Ordena
   * de la donación más nueva a la más vieja; el id desempata para que las
   * páginas sean estables.
   */
  Pagina<Donacion> buscar(FiltroDonaciones filtro, SolicitudPagina pagina);

  Optional<Donacion> buscarPorId(UUID id);

  /**
   * Integridad referencial (Entrega 5): donacion.id_donante e
   * id_entidad_beneficiaria referencian a otros agregados solo por id, sin FK.
   * Antes de borrar una persona o una entidad se consulta si alguna donación la
   * referencia, y en ese caso el borrado se rechaza.
   */
  boolean existenDelDonante(UUID idDonante);

  boolean existenAsignadasA(UUID idEntidad);

  /** Borra la donación y, si estaba asignada, descuenta la recepción de su entidad. */
  void eliminar(UUID idDonacion);
}
