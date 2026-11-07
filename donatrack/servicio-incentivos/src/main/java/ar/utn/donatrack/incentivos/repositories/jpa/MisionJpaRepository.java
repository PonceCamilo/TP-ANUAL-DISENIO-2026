package ar.utn.donatrack.incentivos.repositories.jpa;

import ar.utn.donatrack.incentivos.models.misiones.Mision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MisionJpaRepository extends JpaRepository<Mision, Long> {
    Optional<Mision> findByNombre(String nombre);
    boolean existsByNombre(String nombre);
    long countByCategoriaRequeridaOrden(int orden);
    List<Mision> findByCategoriaRequeridaOrdenOrderByOrdenAsc(int orden);
}
