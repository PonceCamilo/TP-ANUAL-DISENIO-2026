package ar.utn.donatrack.donaciones.interfaces.repositories;

import ar.utn.donatrack.donaciones.util.FiltroDonaciones;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.util.Pagina;
import ar.utn.donatrack.donaciones.util.SolicitudPagina;

import java.time.LocalDate;
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


    /**
     * Un lote de donaciones en ese estado, ordenadas por id, que empieza después de
     * la indicada (null: desde la primera). Los procesos nocturnos las recorren así
     * y no por número de página: si una donación cambia de estado en el medio, el
     * recorrido no saltea ni repite otras.
     */
    List<Donacion> obtenerLoteEnEstado(String estado, UUID despuesDe, int cantidad);

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

    /**
     * Donaciones entregadas entre esas fechas (las dos incluidas), de la entrega
     * más reciente a la más vieja (Entrega 6: la landing muestra las del último mes).
     */
    Pagina<Donacion> buscarEntregadas(LocalDate desde, LocalDate hasta, SolicitudPagina pagina);

    /**
     * Las subcategorías que tienen donaciones, una vez cada una (las que solo
     * difieren en mayúsculas o acentos cuentan como la misma), con la forma en que
     * se escribieron. Sirven de catálogo para los filtros y formularios (Entrega 6).
     */
    List<String> subcategoriasUsadas();
}
