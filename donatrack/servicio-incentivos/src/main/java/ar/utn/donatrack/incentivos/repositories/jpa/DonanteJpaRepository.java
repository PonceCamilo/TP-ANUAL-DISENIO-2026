package ar.utn.donatrack.incentivos.repositories.jpa;

import ar.utn.donatrack.incentivos.models.Donante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DonanteJpaRepository extends JpaRepository<Donante, UUID> {
}
