package ar.utn.donatrack.logistica.models.planificacion;

import ar.utn.donatrack.logistica.models.comun.Direccion;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Snapshot de la donación tal como llegó en el pedido de planificación.
 * Se guarda en el LotePlanificacion para enviarla al proveedor de ruteo
 * (id, entidad destino y dirección). El donante no forma parte de logística:
 * los camiones entregan en la entidad beneficiaria.
 */
@Getter
@Builder
public class DonacionLote {
    private UUID idDonacion;
    private UUID idEntidadBeneficiaria;
    private Direccion direccionEntrega;
}
