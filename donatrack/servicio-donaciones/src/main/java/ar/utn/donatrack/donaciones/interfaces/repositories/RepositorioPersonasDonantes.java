package ar.utn.donatrack.donaciones.interfaces.repositories;


import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.util.Pagina;
import ar.utn.donatrack.donaciones.util.SolicitudPagina;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
/** Persistencia de las personas donantes (humanas y jurídicas). */
public interface RepositorioPersonasDonantes {
    /**
     * Listado paginado ordenado por email (Entrega 5). Si estado es nulo o está
     * en blanco devuelve todas las personas.
     */
    Pagina<PersonaDonante> buscar(String estado, SolicitudPagina pagina);

    /** Hasta `cantidad` personas a las que corresponde avisar por inactividad (ver la consulta). */
    List<PersonaDonante> obtenerParaAvisoDeInactividad(LocalDateTime limite, int cantidad);

    /** Deja registrado que a esas personas se les avisó por inactividad en ese momento. */
    void registrarAvisoDeInactividad(Collection<UUID> ids, LocalDateTime cuando);

    void guardar(PersonaDonante donante);

    /**
     * Guarda varias personas en una sola transacción, con inserciones por lotes
     * (Entrega 5). Lo usa la importación masiva por CSV.
     */
    void guardarTodos(Collection<PersonaDonante> donantes);

    boolean existePorEmail(String email);

    Optional<PersonaDonante> buscarPorId(UUID id);

    /** Varias personas en una sola consulta. Las que no existen simplemente no aparecen. */
    List<PersonaDonante> obtenerPorIds(Collection<UUID> ids);

    /** El email puede venir como lo escribió la persona: se busca por su forma canónica. */
    Optional<PersonaDonante> buscarPorEmail(String email);

    /**
     * Busca en una sola consulta a las personas de varios emails. La clave del
     * mapa es el email en forma canónica (ver NormalizadorEmail). Los emails que
     * no están registrados no aparecen.
     */
    Map<String, PersonaDonante> obtenerPorEmails(Collection<String> emails);

    void eliminar(PersonaDonante persona);
}
