package ar.utn.donatrack.incentivos.interfaces.repositories;

import ar.utn.donatrack.incentivos.models.Donante;
import java.util.Optional;
import java.util.UUID;

public interface DonanteRepositoryInterface {
    Optional<Donante> findById(UUID id);
    Donante save(Donante donante);
}