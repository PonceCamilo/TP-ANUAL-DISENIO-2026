package ar.utn.donatrack.incentivos.repositories;

import ar.utn.donatrack.incentivos.interfaces.repositories.DonanteRepositoryInterface;
import ar.utn.donatrack.incentivos.models.Donante;
import ar.utn.donatrack.incentivos.repositories.jpa.DonanteJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class DonanteRepositorioAdapter implements DonanteRepositoryInterface {

    private final DonanteJpaRepository repositorio;

    public DonanteRepositorioAdapter(DonanteJpaRepository repositorio) {
        this.repositorio = repositorio;
    }

    public Optional<Donante> findById(UUID id) {
        return repositorio.findById(id);
    }

    public Donante save(Donante donante) {
        return repositorio.save(donante);
    }
}
