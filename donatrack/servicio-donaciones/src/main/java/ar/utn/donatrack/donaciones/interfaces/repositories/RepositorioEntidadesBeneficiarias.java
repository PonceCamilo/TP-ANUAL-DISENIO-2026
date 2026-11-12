package ar.utn.donatrack.donaciones.interfaces.repositories;

import ar.utn.donatrack.donaciones.util.SolicitudPagina;
import ar.utn.donatrack.donaciones.util.Pagina;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioEntidadesBeneficiarias {
    void guardar(EntidadBeneficiaria entidad);
    /** Todas: el matchmaking puntúa a cada entidad. */
    List<EntidadBeneficiaria> buscarTodas();

    /** Listado paginado, por razón social; el id desempata para que las páginas sean estables. */
    Pagina<EntidadBeneficiaria> buscar(SolicitudPagina pagina);

    /**
     * Las entidades que tienen ese email como contacto o como email de un
     * representante, con el mismo orden que el listado (Entrega 6). El email
     * puede venir como lo escribió la persona.
     */
    Pagina<EntidadBeneficiaria> buscarPorEmail(String email, SolicitudPagina pagina);
    Optional<EntidadBeneficiaria> buscarPorId(UUID id);

    /** Varias entidades en una sola consulta. Las que no existen simplemente no aparecen. */
    List<EntidadBeneficiaria> obtenerPorIds(Collection<UUID> ids);

    void eliminar(EntidadBeneficiaria entidad);

    /**
     * Las subcategorías que piden las necesidades, una vez cada una, con la forma
     * en que se escribieron (Entrega 6: catálogo para filtros y formularios).
     */
    List<String> subcategoriasDeNecesidades();
}
