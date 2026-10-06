package ar.utn.donatrack.brokerlogistica.repositories;

import ar.utn.donatrack.brokerlogistica.dtos.EnvioAsignadoDTO;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Qué proveedor tomó cada donación (en memoria, como el resto de los servicios). */
@Repository
public class RegistroEnviosRepository {

    private final ConcurrentHashMap<UUID, EnvioAsignadoDTO> storage = new ConcurrentHashMap<>();

    public void guardar(EnvioAsignadoDTO envio) {
        storage.put(envio.idDonacion(), envio);
    }

    public Optional<EnvioAsignadoDTO> buscarPorDonacion(UUID idDonacion) {
        return Optional.ofNullable(storage.get(idDonacion));
    }
}
