package ar.utn.donatrack.logistica.repositories;

import ar.utn.donatrack.logistica.interfaces.repositories.CamionRepositoryInterface;
import ar.utn.donatrack.logistica.models.flota.Camion;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
public class CamionRepository extends RepositorioJpa<Camion> implements CamionRepositoryInterface {

    public CamionRepository() {
        super(Camion.class);
    }

    @Override
    public void guardar(Camion camion) {
        guardarEntidad(camion, camion.getId());
    }

    @Override
    public List<Camion> buscarTodos() {
        return em.createQuery("SELECT c FROM Camion c ORDER BY c.patente", Camion.class)
                .getResultList();
    }

    @Override
    public Camion buscarPorId(UUID id) {
        return buscarEntidad(id);
    }

    @Override
    public Camion buscarPorPatente(String patente) {
        return em.createQuery("SELECT c FROM Camion c WHERE c.patente = :patente", Camion.class)
                .setParameter("patente", patente)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    /**
     * Devuelve los camiones encontrados respetando el orden de los ids pedidos,
     * sin repetir (un camión aparece una sola vez por lote en lote_camion).
     */
    @Override
    public List<Camion> buscarPorIds(List<UUID> ids) {
        List<UUID> idsValidos = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (idsValidos.isEmpty()) {
            return List.of();
        }
        Map<UUID, Camion> porId = em.createQuery("SELECT c FROM Camion c WHERE c.id IN :ids", Camion.class)
                .setParameter("ids", idsValidos)
                .getResultStream()
                .collect(Collectors.toMap(Camion::getId, Function.identity()));
        return idsValidos.stream()
                .map(porId::get)
                .filter(Objects::nonNull)
                .toList();
    }
}
