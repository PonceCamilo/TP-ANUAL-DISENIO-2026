package ar.utn.donatrack.logistica.dtos.response;

import ar.utn.donatrack.logistica.models.planificacion.EstadoLote;
import ar.utn.donatrack.logistica.models.planificacion.LotePlanificacion;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class LoteResponseDTO {
    private UUID id;
    private EstadoLote estado;
    private int cantidadDonaciones;
    private LocalDateTime fechaEnvio;
    private LocalDateTime fechaRespuesta;

    public static LoteResponseDTO desde(LotePlanificacion lote) {
        return LoteResponseDTO.builder()
                .id(lote.getId())
                .estado(lote.getEstado())
                .cantidadDonaciones(lote.getDonaciones().size())
                .fechaEnvio(lote.getFechaEnvio())
                .fechaRespuesta(lote.getFechaRespuesta())
                .build();
    }
}
