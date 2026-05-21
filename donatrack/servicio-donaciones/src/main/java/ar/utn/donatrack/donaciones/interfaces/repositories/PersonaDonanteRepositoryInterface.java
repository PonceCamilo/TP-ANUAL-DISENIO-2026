package ar.utn.donatrack.donaciones.interfaces.repositories;

import ar.utn.donatrack.donaciones.model.donante.PersonaDonante;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Interfaz mínima para persistir y buscar donantes desde el servicio de importación.
 * Implementaciones existentes del proyecto pueden adaptarse a esta interfaz.
 */
public interface PersonaDonanteRepositoryInterface {
    PersonaDonante obtenerPorMail(String email);
    PersonaDonante obtenerPorId(UUID id);
    List<PersonaDonante> obtenerTodosDonantes();
    List<PersonaDonante> obtenerTodosActivos();
    void darDeBaja(UUID id);
    void guardar(PersonaDonante donante);
    void reactivar(UUID id);

}
