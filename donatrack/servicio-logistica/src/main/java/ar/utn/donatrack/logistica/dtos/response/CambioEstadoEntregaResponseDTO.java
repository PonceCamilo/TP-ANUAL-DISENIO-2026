package ar.utn.donatrack.logistica.dtos.response;

import ar.utn.donatrack.logistica.models.entrega.CambioEstadoEntrega;
import ar.utn.donatrack.logistica.models.entrega.EstadoEntrega;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CambioEstadoEntregaResponseDTO {
    private EstadoEntrega estado;
    private String observacion;
    private LocalDateTime fechaHora;

    public static CambioEstadoEntregaResponseDTO desde(CambioEstadoEntrega cambio) {
        return CambioEstadoEntregaResponseDTO.builder()
                .estado(cambio.getEstado())
                .observacion(cambio.getObservacion())
                .fechaHora(cambio.getFechaHora())
                .build();
    }
}
