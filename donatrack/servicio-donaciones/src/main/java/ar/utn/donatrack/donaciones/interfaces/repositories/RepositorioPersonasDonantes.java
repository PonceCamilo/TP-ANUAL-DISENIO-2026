package ar.utn.donatrack.donaciones.interfaces.repositories;

import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.donante.Representante;

import ar.utn.donatrack.donaciones.util.Pagina;
import ar.utn.donatrack.donaciones.util.SolicitudPagina;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
/**
 * Interfaz mínima para persistir y buscar donantes desde el servicio de importación.
 * Implementaciones existentes del proyecto pueden adaptarse a esta interfaz.
 */
public interface RepositorioPersonasDonantes {
    /**
     * Listado paginado ordenado por email (Entrega 5). Si estado es nulo o está
     * en blanco devuelve todas las personas.
     */
    Pagina<PersonaDonante> buscar(String estado, SolicitudPagina pagina);

    /**
     * Personas sin interacción desde la fecha indicada, o que nunca interactuaron.
     * El filtro lo resuelve la base con el índice de ultima_interaccion: el
     * proceso nocturno ya no carga la tabla entera.
     */
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
    void modificarRepresentante(UUID idDonante, Representante representante);
    boolean existePorId(UUID id);
    boolean existePorEmail(String email);
    PersonaDonante obtenerPersona(UUID id);
    PersonaDonante obtenerPorEmail(String email);

    /**
     * Busca en una sola consulta a las personas de varios emails. La clave del
     * mapa es el email en forma canónica (ver NormalizadorEmail). Los emails que
     * no están registrados no aparecen.
     */
    Map<String, PersonaDonante> obtenerPorEmails(Collection<String> emails);
    void eliminar(UUID id);
}
