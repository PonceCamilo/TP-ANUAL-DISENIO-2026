package ar.utn.donatrack.logistica.models.planificacion;

import ar.utn.donatrack.logistica.models.entrega.Entrega;
import ar.utn.donatrack.logistica.models.flota.Camion;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Raíz del agregado Ruta → Parada → Entrega: guardar la ruta guarda en
 * cascada sus paradas y las entregas de cada parada.
 */
@Entity
@Table(name = "ruta")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Ruta {
    @Id
    @Column(name = "id_ruta")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_lote", nullable = false)
    private LotePlanificacion lote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_camion", nullable = false)
    private Camion camion;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "id_ruta", nullable = false)
    @OrderBy("orden")
    private List<Parada> paradas;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
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
