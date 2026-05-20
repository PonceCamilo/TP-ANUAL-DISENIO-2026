package ar.utn.donatrack.donaciones.interfaces.repositories;

import ar.utn.donatrack.donaciones.model.donante.PersonaDonante;
import java.util.Optional;

/**
 * Interfaz mínima para persistir y buscar donantes desde el servicio de importación.
 * Implementaciones existentes del proyecto pueden adaptarse a esta interfaz.
 */
public interface DonanteRepositoryInterface {
    Optional<PersonaDonante> findByEmail(String email);
    void save(PersonaDonante donante);
}
