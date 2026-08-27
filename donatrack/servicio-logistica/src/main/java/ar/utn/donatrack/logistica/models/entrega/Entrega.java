package ar.utn.donatrack.logistica.models.entrega;

import ar.utn.donatrack.logistica.models.planificacion.Parada;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Una entrega pertenece a una parada física. No conoce Ruta ni
 * Camión: esos se resuelven por query inversa sobre el agregado
 * Ruta → Parada → Entrega. La donación ajena se referencia solo por id.
 */
@Getter
@Setter
@Builder
public class Entrega {
    private UUID id;
    private UUID idDonacion;
    private Parada parada;

    @Builder.Default
    private EstadoEntrega estado = EstadoEntrega.PENDIENTE;

    @Builder.Default
    private List<CambioEstadoEntrega> historial = new ArrayList<>();

    @Builder.Default
    private List<String> fotosComprobante = new ArrayList<>();

    private String observacion;
    private LocalDateTime fechaEntrega;

    public void registrarCambio(EstadoEntrega nuevoEstado, String observacion) {
        this.estado = nuevoEstado;
        this.observacion = observacion;
        this.historial.add(CambioEstadoEntrega.builder()
                .estado(nuevoEstado)
                .observacion(observacion)
                .build());
    }
}
