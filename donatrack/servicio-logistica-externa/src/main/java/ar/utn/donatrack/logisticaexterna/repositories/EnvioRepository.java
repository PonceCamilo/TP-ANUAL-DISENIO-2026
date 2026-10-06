package ar.utn.donatrack.logisticaexterna.repositories;

import ar.utn.donatrack.logisticaexterna.models.Envio;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Datos propios de este proveedor: no comparte almacenamiento con servicio-logistica. */
@Repository
public class EnvioRepository {

    private final ConcurrentHashMap<String, Envio> storage = new ConcurrentHashMap<>();

    public void guardar(Envio envio) {
        storage.put(envio.getTrackingId(), envio);
    }

    public Optional<Envio> buscarPorTracking(String trackingId) {
        return Optional.ofNullable(storage.get(trackingId));
    }

    public List<Envio> buscarTodos() {
        return new ArrayList<>(storage.values());
    }
}
