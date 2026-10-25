package ar.utn.donatrack.logistica.models.planificacion;

import ar.utn.donatrack.logistica.models.entrega.Entrega;
import ar.utn.donatrack.logistica.models.flota.Camion;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Documento de la colección rutas. Paradas, direcciones, entregas,
 * historial y fotos van embebidos. El lote y el camión se referencian por id.
 */
@Document(collection = "rutas")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Ruta {
    @Id
    private UUID id;

    private UUID loteId;

    private UUID camionId;

    /** Lo completa RutaRepository al leer. No se embebe el camión. */
    @Transient
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

    /** Rearma el puntero entrega → parada, que el documento no guarda. */
    public void enlazarEntregas() {
        if (paradas == null) {
            return;
        }
        for (Parada parada : paradas) {
            if (parada.getEntregas() == null) {
                continue;
            }
            for (Entrega entrega : parada.getEntregas()) {
                entrega.setParada(parada);
            }
        }
    }
}
