package ar.utn.donatrack.logistica.models.entrega;

import ar.utn.donatrack.logistica.models.planificacion.Parada;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Una entrega pertenece a una parada física; la ruta, el camión y la entidad
 * beneficiaria se obtienen a través de ella. La donación es de
 * servicio-donaciones: se referencia solo por id, sin foreign key.
 */
@Entity
@Table(name = "entrega")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Entrega {
    @Id
    @Column(name = "id_entrega")
    private UUID id;

    @Column(nullable = false)
    private UUID idDonacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_parada", nullable = false)
    private Parada parada;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoEntrega estado = EstadoEntrega.LISTO_PARA_ENTREGAR;

    @Builder.Default
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "id_entrega", nullable = false)
    @OrderBy("fechaHora")
    private List<CambioEstadoEntrega> historial = new ArrayList<>();

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "foto_comprobante_entrega", joinColumns = @JoinColumn(name = "id_entrega"))
    @OrderColumn(name = "posicion")
    @Column(name = "url_foto", nullable = false, length = 2048)
    private List<String> fotosComprobante = new ArrayList<>();

    @Column(columnDefinition = "text")
    private String observacion;

    private LocalDateTime fechaEntrega;

    public void registrarCambio(EstadoEntrega nuevoEstado, String observacion) {
        registrarCambio(nuevoEstado, null, observacion);
    }

    public void registrarCambio(EstadoEntrega nuevoEstado, MotivoFalloEntrega motivoFallo, String observacion) {
        this.estado = nuevoEstado;
        this.observacion = observacion;
        this.historial.add(CambioEstadoEntrega.builder()
                .estado(nuevoEstado)
                .motivoFallo(motivoFallo)
                .observacion(observacion)
                .build());
    }
}
