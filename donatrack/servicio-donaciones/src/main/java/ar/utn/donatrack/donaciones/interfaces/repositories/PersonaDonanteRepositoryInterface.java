package ar.utn.donatrack.donaciones.interfaces.repositories;

import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.donante.Representante;

import ar.utn.donatrack.donaciones.util.Pagina;
import ar.utn.donatrack.donaciones.util.SolicitudPagina;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
/**
 * Interfaz mínima para persistir y buscar donantes desde el servicio de importación.
 * Implementaciones existentes del proyecto pueden adaptarse a esta interfaz.
 */
public interface PersonaDonanteRepositoryInterface {
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
    List<PersonaDonante> obtenerInactivasDesde(LocalDateTime limite);
    void guardar(PersonaDonante donante);
    void modificarRepresentante(UUID idDonante, Representante representante);
    boolean existePorId(UUID id);
    boolean existePorEmail(String email);
    PersonaDonante obtenerPersona(UUID id);
    PersonaDonante obtenerPorEmail(String email);
    void eliminar(UUID id);
}
