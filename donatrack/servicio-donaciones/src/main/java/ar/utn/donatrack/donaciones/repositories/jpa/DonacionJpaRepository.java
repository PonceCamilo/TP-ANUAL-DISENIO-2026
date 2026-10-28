package ar.utn.donatrack.donaciones.repositories.jpa;

import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

/**
 * Acceso JPA a la tabla donacion.
 *
 * Es deliberadamente un detalle interno del paquete repositories: los services
 * dependen de RepositorioDonaciones, no de esta interfaz. Así el día que
 * cambie la tecnología de persistencia, el cambio no sale de este paquete.
 *
 * Las consultas se derivan del nombre del método; el estado se filtra por la
 * columna `estado`, que guarda el NOMBRE del objeto de estado. Los filtros
 * combinables del listado se arman con Specification (JpaSpecificationExecutor).
 */
public interface DonacionJpaRepository extends JpaRepository<Donacion, UUID>, JpaSpecificationExecutor<Donacion> {

    List<Donacion> findByEstadoNombre(String estadoNombre);

    boolean existsByIdDonante(UUID idDonante);

    boolean existsByIdEntidadBeneficiaria(UUID idEntidadBeneficiaria);
}
