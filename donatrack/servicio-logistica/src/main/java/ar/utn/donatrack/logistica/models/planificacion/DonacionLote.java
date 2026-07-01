package ar.utn.donatrack.logistica.models.planificacion;

import ar.utn.donatrack.logistica.models.comun.Direccion;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Snapshot de la donación tal como llegó en el pedido de planificación.
 * Se guarda dentro del LotePlanificacion para poder reconstruir, cuando
 * llega el callback del proveedor externo (que solo agrupa donacionesIds
 * por parada), a qué entidad y a qué donante pertenece cada una.
 */
@Getter
@Builder
public class DonacionLote {
    private UUID idDonacion;
    private UUID idEntidadBeneficiaria;
    private UUID idDonante;
    private Direccion direccionEntrega;
}
