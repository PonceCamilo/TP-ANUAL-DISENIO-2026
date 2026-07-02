package ar.utn.donatrack.logistica.models.planificacion;

import ar.utn.donatrack.logistica.models.comun.Direccion;
import ar.utn.donatrack.logistica.models.entrega.Entrega;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class Parada {
    private UUID id;
    private int orden;
    private Direccion direccion;
    private UUID idEntidadBeneficiaria;
    private List<Entrega> entregas;
}
