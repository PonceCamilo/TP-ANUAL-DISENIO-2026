package ar.utn.donatrack.incentivos.repositories.jpa;

import ar.utn.donatrack.incentivos.models.Donante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface DonanteJpaRepository extends JpaRepository<Donante, UUID> {

    /** Solo id y nombre visible: el podio y el ranking no necesitan el perfil entero. */
    @Query("select d.id, d.nombreVisible from Donante d where d.id in :ids")
    List<Object[]> nombresVisibles(@Param("ids") Collection<UUID> ids);
}
