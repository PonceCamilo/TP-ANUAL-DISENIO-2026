package ar.utn.donatrack.incentivos.repositories;

import ar.utn.donatrack.incentivos.interfaces.repositories.DonanteRepositoryInterface;
import ar.utn.donatrack.incentivos.models.Donante;
import ar.utn.donatrack.incentivos.repositories.impl.IncentivosRepositorioEnMemoria;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapta el repositorio en memoria existente (IncentivosRepositorioEnMemoria)
 * a la interfaz DonanteRepositoryInterface que consume DonanteService.
 * De esta forma ambos comparten el mismo almacén de perfiles de donante.
 */
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
