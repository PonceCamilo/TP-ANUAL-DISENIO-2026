package ar.utn.donatrack.logistica.dtos.response;

import ar.utn.donatrack.logistica.models.entrega.Entrega;
import ar.utn.donatrack.logistica.models.entrega.EstadoEntrega;
import ar.utn.donatrack.logistica.models.flota.Camion;
import ar.utn.donatrack.logistica.models.planificacion.Ruta;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class EntregaResponseDTO {
    private UUID id;
    private UUID idDonacion;
    private UUID rutaId;
    private UUID camionId;
    private EstadoEntrega estado;
    private List<String> fotosComprobante;
    private String observacion;
    private LocalDateTime fechaEntrega;
    private List<CambioEstadoEntregaResponseDTO> historial;

    public static EntregaResponseDTO desde(Entrega entrega) {
        return desde(entrega, null);
    }

    public static EntregaResponseDTO desde(Entrega entrega, Ruta ruta) {
        Camion camion = ruta != null ? ruta.getCamion() : null;
        return EntregaResponseDTO.builder()
                .id(entrega.getId())
                .idDonacion(entrega.getIdDonacion())
                .rutaId(ruta != null ? ruta.getId() : null)
                .camionId(camion != null ? camion.getId() : null)
                .estado(entrega.getEstado())
                .fotosComprobante(entrega.getFotosComprobante())
                .observacion(entrega.getObservacion())
                .fechaEntrega(entrega.getFechaEntrega())
                .historial(entrega.getHistorial().stream().map(CambioEstadoEntregaResponseDTO::desde).toList())
                .build();
    }
}
