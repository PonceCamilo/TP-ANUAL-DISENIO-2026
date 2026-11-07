package ar.utn.donatrack.logistica.dtos.response;

import ar.utn.donatrack.logistica.models.planificacion.EstadoRuta;
import ar.utn.donatrack.logistica.models.planificacion.Ruta;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class RutaResponseDTO {
    private UUID id;
    private UUID camionId;
    private EstadoRuta estado;
    private LocalDateTime fechaInicio;
    private List<ParadaResponseDTO> paradas;

    public static RutaResponseDTO desde(Ruta ruta) {
        return RutaResponseDTO.builder()
                .id(ruta.getId())
                .camionId(ruta.getCamion() != null ? ruta.getCamion().getId() : null)
                .estado(ruta.getEstado())
                .fechaInicio(ruta.getFechaInicio())
                .paradas(ruta.getParadas().stream().map(ParadaResponseDTO::desde).toList())
                .build();
    }
}
