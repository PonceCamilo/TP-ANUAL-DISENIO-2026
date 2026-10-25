package ar.utn.donatrack.logistica.models.planificacion;

import ar.utn.donatrack.logistica.models.comun.Direccion;
import ar.utn.donatrack.logistica.models.entrega.Entrega;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;
import java.util.UUID;

/**
 * Parada embebida en la ruta. El id se conserva porque la API lo muestra;
 * no es el _id de un documento.
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Parada {
    @Field("id")
    private UUID id;

    private int orden;
    private Direccion direccion;

    // Id de la entidad beneficiaria de servicio-donaciones, sin relación hacia esa base.
    private UUID idEntidadBeneficiaria;

    private List<Entrega> entregas;

    public void reemplazarEntrega(Entrega entrega) {
        if (entregas == null) {
            return;
        }
        for (int i = 0; i < entregas.size(); i++) {
            if (entrega.getId().equals(entregas.get(i).getId())) {
                entregas.set(i, entrega);
                entrega.setParada(this);
                return;
            }
        }
    }
}
