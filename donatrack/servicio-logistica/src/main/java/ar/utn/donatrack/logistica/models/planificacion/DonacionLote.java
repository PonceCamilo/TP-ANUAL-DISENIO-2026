package ar.utn.donatrack.logistica.models.planificacion;

import ar.utn.donatrack.logistica.models.comun.Direccion;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Donación embebida en el lote, tal como llegó en el pedido de planificación.
 * No tiene id propio: se identifica por idDonacion, que es de servicio-donaciones
 * y se guarda sin relación hacia esa base. El donante no forma parte de logística.
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class DonacionLote {
    private UUID idDonacion;
    private UUID idEntidadBeneficiaria;
    private Direccion direccionEntrega;
}
