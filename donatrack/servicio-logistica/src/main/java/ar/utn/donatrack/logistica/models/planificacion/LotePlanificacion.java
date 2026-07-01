package ar.utn.donatrack.logistica.models.planificacion;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Representa un envío al proveedor externo de ruteo (máximo 100 donaciones,
 * impuesto por PlanificacionRutasService antes de crear el lote).
 */
@Getter
@Setter
@Builder
public class LotePlanificacion {
    private UUID id;
    private List<UUID> camionesIds;
    private List<DonacionLote> donaciones;
    private EstadoLote estado;
    private String tokenCorrelacion;
    private LocalDateTime fechaEnvio;
    private LocalDateTime fechaRespuesta;
}
