package ar.utn.donatrack.incentivos.repositories.jpa;

import ar.utn.donatrack.incentivos.models.categoriasdonante.CategoriaDonante;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaDonanteJpaRepository extends JpaRepository<CategoriaDonante, Integer> {
}
