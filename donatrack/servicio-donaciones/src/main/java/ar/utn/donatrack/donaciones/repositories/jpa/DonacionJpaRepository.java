package ar.utn.donatrack.donaciones.repositories.jpa;

import org.springframework.data.jpa.repository.QueryHints;
import jakarta.persistence.QueryHint;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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

    /** Asignación que tiene hoy una donación en la base, antes de guardar un cambio. */
    interface AsignacionActual {
        UUID getIdEntidadBeneficiaria();

        LocalDate getFechaAsignacion();
    }

    /**
     * Se lee sin vaciar antes los cambios pendientes (flush mode COMMIT): la
     * donación que se está guardando puede estar gestionada y ya modificada, y un
     * vaciado automático escribiría la asignación nueva antes de esta consulta.
     */
    @QueryHints(@QueryHint(name = "org.hibernate.flushMode", value = "COMMIT"))
    @Query("SELECT d.idEntidadBeneficiaria AS idEntidadBeneficiaria, d.fechaAsignacion AS fechaAsignacion "
            + "FROM Donacion d WHERE d.id = :id")
    Optional<AsignacionActual> buscarAsignacion(@Param("id") UUID id);
}
