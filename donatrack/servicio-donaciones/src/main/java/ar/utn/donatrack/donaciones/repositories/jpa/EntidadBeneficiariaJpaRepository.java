package ar.utn.donatrack.donaciones.repositories.jpa;

import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Acceso JPA a la tabla entidad_beneficiaria y, por cascada, a sus campañas y necesidades. */
public interface EntidadBeneficiariaJpaRepository extends JpaRepository<EntidadBeneficiaria, UUID> {
}
