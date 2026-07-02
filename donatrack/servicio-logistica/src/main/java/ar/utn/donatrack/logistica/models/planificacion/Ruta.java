package ar.utn.donatrack.logistica.models.planificacion;

import ar.utn.donatrack.logistica.models.flota.Camion;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
public class Ruta {
    private UUID id;
    private LotePlanificacion lote;
    private Camion camion;
    private List<Parada> paradas;
    private EstadoRuta estado;
    private LocalDateTime fechaInicio;
}
