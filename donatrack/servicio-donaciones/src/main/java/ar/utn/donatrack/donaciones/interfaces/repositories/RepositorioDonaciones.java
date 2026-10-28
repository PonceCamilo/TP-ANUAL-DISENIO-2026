package ar.utn.donatrack.donaciones.interfaces.repositories;

import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.util.Pagina;
import ar.utn.donatrack.donaciones.util.SolicitudPagina;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioDonaciones {
  void cargarDonaciones(List<Donacion> donaciones);

  /**
   * Guarda una donación nueva o los cambios de una existente. Si cambió su
   * asignación, actualiza en la misma transacción el contador de recepciones de
   * las entidades: quien llama no tiene que elegir otro método al asignar.
   */
  void guardar(Donacion donacion);


  List<Donacion> obtenerPorEstado(String estado);

  /**
   * Listado paginado con los filtros aplicados en la base (Entrega 5). Ordena
   * de la donación más nueva a la más vieja; el id desempata para que las
   * páginas sean estables.
   */
  Pagina<Donacion> buscar(FiltroDonaciones filtro, SolicitudPagina pagina);

  Optional<Donacion> buscarPorId(UUID id);

  /** Varias donaciones en una sola consulta. Las que no existen simplemente no aparecen. */
  List<Donacion> obtenerPorIds(Collection<UUID> ids);

  /**
   * Integridad referencial (Entrega 5): donacion.id_donante e
   * id_entidad_beneficiaria referencian a otros agregados solo por id, sin FK.
   * Antes de borrar una persona o una entidad se consulta si alguna donación la
   * referencia, y en ese caso el borrado se rechaza.
   */
  boolean existenDelDonante(UUID idDonante);

  boolean existenAsignadasA(UUID idEntidad);

  /** Borra la donación y, si estaba asignada, descuenta la recepción de su entidad. */
}
