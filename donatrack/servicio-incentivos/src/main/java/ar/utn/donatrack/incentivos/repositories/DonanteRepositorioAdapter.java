package ar.utn.donatrack.incentivos.repositories;

import ar.utn.donatrack.incentivos.interfaces.repositories.DonanteRepositoryInterface;
import ar.utn.donatrack.incentivos.models.Donante;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class DonanteRepositorioAdapter implements DonanteRepositoryInterface {

    private final IncentivosRepositorioEnMemoria repositorio;

    public DonanteRepositorioAdapter(IncentivosRepositorioEnMemoria repositorio) {
        this.repositorio = repositorio;
    }

    public Optional<Donante> findById(UUID id) {
        return repositorio.buscarPerfil(id);
    }

    public Donante save(Donante donante) {
        repositorio.guardarPerfil(donante);
        return donante;
    }
}
