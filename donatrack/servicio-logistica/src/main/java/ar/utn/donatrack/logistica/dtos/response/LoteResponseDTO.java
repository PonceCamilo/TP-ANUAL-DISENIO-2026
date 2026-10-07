package ar.utn.donatrack.logistica.dtos.response;

import ar.utn.donatrack.logistica.models.planificacion.EstadoLote;
import ar.utn.donatrack.logistica.models.planificacion.LotePlanificacion;
import ar.utn.donatrack.logistica.models.planificacion.Ruta;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class LoteResponseDTO {
    private UUID id;
    private EstadoLote estado;
    private int cantidadDonaciones;
    private LocalDateTime fechaEnvio;
    private LocalDateTime fechaRespuesta;
    private List<RutaResponseDTO> rutas;

    public static LoteResponseDTO desde(LotePlanificacion lote, List<Ruta> rutas) {
        return LoteResponseDTO.builder()
                .id(lote.getId())
                .estado(lote.getEstado())
                .cantidadDonaciones(lote.getDonaciones().size())
                .fechaEnvio(lote.getFechaEnvio())
                .fechaRespuesta(lote.getFechaRespuesta())
                .rutas(rutas.stream().map(RutaResponseDTO::desde).toList())
                .build();
    }

    public static LoteResponseDTO desde(LotePlanificacion lote) {
        return desde(lote, List.of());
    }
}
