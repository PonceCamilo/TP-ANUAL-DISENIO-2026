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
    Optional<EntidadBeneficiaria> buscarPorId(UUID id);

    /** Varias entidades en una sola consulta. Las que no existen simplemente no aparecen. */
    List<EntidadBeneficiaria> obtenerPorIds(Collection<UUID> ids);

    void eliminar(EntidadBeneficiaria entidad);
}