package ar.utn.donatrack.donaciones.repositories.jpa;

import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Acceso JPA a la tabla donacion.
 *
 * Es deliberadamente un detalle interno del paquete repositories: los services
 * dependen de DonacionesRepositoryInterface, no de esta interfaz. Así el día que
 * cambie la tecnología de persistencia, el cambio no sale de este paquete.
 *
 * Las consultas se derivan del nombre del método; el estado se filtra por la
 * columna `estado`, que guarda el NOMBRE del objeto de estado.
 */
public interface DonacionJpaRepository extends JpaRepository<Donacion, UUID> {

    List<Donacion> findByEstadoNombre(String estadoNombre);

    List<Donacion> findByIdDonante(UUID idDonante);
}
