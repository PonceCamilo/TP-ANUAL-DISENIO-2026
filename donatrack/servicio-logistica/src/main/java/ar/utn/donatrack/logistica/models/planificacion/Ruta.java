package ar.utn.donatrack.logistica.models.planificacion;

import ar.utn.donatrack.logistica.models.entrega.Entrega;
import ar.utn.donatrack.logistica.models.flota.Camion;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Getter
@Setter
@Builder
public class Ruta {
    private UUID id;
    private LotePlanificacion lote;
    private Camion camion;
    private List<Parada> paradas;
    private EstadoRuta estado;
    private LocalDateTime fechaInicio;

    public List<Entrega> obtenerEntregas() {
        if (paradas == null) {
            return List.of();
        }
        return paradas.stream()
                .filter(p -> p.getEntregas() != null)
                .flatMap(p -> p.getEntregas().stream())
                .toList();
    }

    public Optional<Parada> buscarParadaPorEntregaId(UUID entregaId) {
        if (paradas == null) {
            return Optional.empty();
        }
        return paradas.stream()
                .filter(p -> p.getEntregas() != null
                        && p.getEntregas().stream().anyMatch(e -> entregaId.equals(e.getId())))
                .findFirst();
    }
}
