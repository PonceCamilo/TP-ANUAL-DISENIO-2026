package ar.utn.donatrack.logistica.models.planificacion;

import ar.utn.donatrack.logistica.models.comun.Direccion;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Snapshot de la donación tal como llegó en el pedido de planificación.
 * Se guarda en el LotePlanificacion para enviarla al proveedor de ruteo
 * (id, entidad destino y dirección). El donante no forma parte de logística:
 * los camiones entregan en la entidad beneficiaria.
 *
 * idDonacion e idEntidadBeneficiaria son ids de servicio-donaciones; en la base
 * apuntan a las tablas de referencia donacion y entidad_beneficiaria.
 */
@Entity
@Table(name = "donacion_lote")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class DonacionLote {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_donacion_lote")
    private UUID id;

    @Column(nullable = false)
    private UUID idDonacion;

    @Column(nullable = false)
    private UUID idEntidadBeneficiaria;

    @ManyToOne(cascade = CascadeType.PERSIST, optional = false)
    @JoinColumn(name = "id_direccion_entrega", nullable = false)
    private Direccion direccionEntrega;
}
