package ar.utn.donatrack.logistica.models.planificacion;

import ar.utn.donatrack.logistica.models.flota.Camion;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Representa un envío al proveedor externo de ruteo (máximo 100 donaciones,
 * impuesto por PlanificacionRutasService antes de crear el lote).
 */
@Entity
@Table(name = "lote_planificacion")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class LotePlanificacion {
    @Id
    @Column(name = "id_lote")
    private UUID id;

    @ManyToMany
    @JoinTable(name = "lote_camion",
            joinColumns = @JoinColumn(name = "id_lote"),
            inverseJoinColumns = @JoinColumn(name = "id_camion"))
    private List<Camion> camiones;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "id_lote", nullable = false)
    private List<DonacionLote> donaciones;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoLote estado;

    @Column(nullable = false, unique = true, length = 100)
    private String tokenCorrelacion;

    @Column(nullable = false)
    private LocalDateTime fechaEnvio;

    private LocalDateTime fechaRespuesta;
}
