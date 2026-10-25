package ar.utn.donatrack.logistica.models.entrega;

import ar.utn.donatrack.logistica.models.planificacion.Parada;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entrega embebida en una parada. El id se conserva: es el seguimiento que
 * el broker le pasa a Donaciones. La donación se referencia solo por id.
 * La parada no se guarda adentro de la entrega (sería un ciclo); se vuelve
 * a enlazar al leer la ruta.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Entrega {
    @Field("id")
    private UUID id;

    private UUID idDonacion;

    @Transient
    private Parada parada;

    @Builder.Default
    private EstadoEntrega estado = EstadoEntrega.LISTO_PARA_ENTREGAR;

    @Builder.Default
    private List<CambioEstadoEntrega> historial = new ArrayList<>();

    @Builder.Default
    private List<String> fotosComprobante = new ArrayList<>();

    private String observacion;

    private LocalDateTime fechaEntrega;

    public void registrarCambio(EstadoEntrega nuevoEstado, String observacion) {
        registrarCambio(nuevoEstado, null, observacion);
    }

    public void registrarCambio(EstadoEntrega nuevoEstado, MotivoFalloEntrega motivoFallo, String observacion) {
        this.estado = nuevoEstado;
        this.observacion = observacion;
        if (historial == null) {
            historial = new ArrayList<>();
        }
        this.historial.add(CambioEstadoEntrega.builder()
                .estado(nuevoEstado)
                .motivoFallo(motivoFallo)
                .observacion(observacion)
                .build());
    }
}
