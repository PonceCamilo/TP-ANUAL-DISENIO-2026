package ar.utn.donatrack.logistica.models.planificacion;

import ar.utn.donatrack.logistica.models.comun.Direccion;
import ar.utn.donatrack.logistica.models.entrega.Entrega;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "parada")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Parada {
    @Id
    @Column(name = "id_parada")
    private UUID id;

    @Column(nullable = false)
    private int orden;

    @ManyToOne(cascade = CascadeType.PERSIST, optional = false)
    @JoinColumn(name = "id_direccion", nullable = false)
    private Direccion direccion;

    // Id de la entidad beneficiaria de servicio-donaciones (sin foreign key).
    @Column(nullable = false)
    private UUID idEntidadBeneficiaria;

    @OneToMany(mappedBy = "parada", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Entrega> entregas;
}
