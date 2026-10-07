package ar.utn.donatrack.logistica.models.entrega;

import ar.utn.donatrack.logistica.models.flota.Camion;
import ar.utn.donatrack.logistica.models.planificacion.Parada;
import ar.utn.donatrack.logistica.models.planificacion.Ruta;
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
 * Una entrega pertenece a una parada física. Además de la parada, guarda la
 * ruta y el camión que la llevan (como en el DER), que se completan al armar
 * la ruta. La donación y la entidad beneficiaria son de servicio-donaciones:
 * se referencian solo por id (tablas de referencia donacion y entidad_beneficiaria).
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

    @Column(nullable = false)
    private UUID idEntidadBeneficiaria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_parada", nullable = false)
    private Parada parada;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_ruta", nullable = false)
    private Ruta ruta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_camion", nullable = false)
    private Camion camion;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoEntrega estado = EstadoEntrega.LISTO_PARA_ENTREGAR;

    @Builder.Default
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "id_entrega", nullable = false)
    @OrderBy("fechaHora")
    private List<CambioEstadoEntrega> historial = new ArrayList<>();

    // No está en el DER: las fotos de comprobante de la confirmación.
    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "entrega_foto", joinColumns = @JoinColumn(name = "id_entrega"))
    @OrderColumn(name = "posicion")
    @Column(name = "url", nullable = false, length = 2048)
    private List<String> fotosComprobante = new ArrayList<>();

    @Column(columnDefinition = "text")
    private String observacion;

    private LocalDateTime fechaEntrega;

    public void registrarCambio(EstadoEntrega nuevoEstado, String observacion) {
        this.estado = nuevoEstado;
        this.observacion = observacion;
        this.historial.add(CambioEstadoEntrega.builder()
                .estado(nuevoEstado)
                .observacion(observacion)
                .build());
    }
}
