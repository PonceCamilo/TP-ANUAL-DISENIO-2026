package ar.utn.donatrack.donaciones.interfaces.repositories;

import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.util.Pagina;
import ar.utn.donatrack.donaciones.util.SolicitudPagina;

import java.util.List;
import java.util.UUID;

public interface DonacionesRepositoryInterface {
  void cargarDonaciones(List<Donacion> donaciones);

  /**
   * Persiste los cambios de una donación ya existente.
   *
   * Es obligatorio llamarlo después de mutar una donación recuperada del
   * repositorio. Con el almacenamiento en memoria anterior alcanzaba con mutar
   * el objeto —era el mismo que estaba en el Map—, pero con JPA la entidad que
   * devuelve obtenerPorId() queda detached y los cambios se pierden si nadie
   * los guarda.
   */
  void guardar(Donacion donacion);

  List<Donacion> obtenerTodas();
  List<Donacion> obtenerPorEstado(String estado);

  /**
   * Listado paginado con los filtros aplicados en la base (Entrega 5). Ordena
   * de la donación más nueva a la más vieja; el id desempata para que las
   * páginas sean estables.
   */
  Pagina<Donacion> buscar(FiltroDonaciones filtro, SolicitudPagina pagina);

  Donacion obtenerPorId(UUID id);
  void eliminar(UUID idDonacion);
}
