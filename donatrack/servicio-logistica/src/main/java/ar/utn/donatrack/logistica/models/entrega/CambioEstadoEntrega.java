package ar.utn.donatrack.logistica.models.entrega;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CambioEstadoEntrega {
    private final EstadoEntrega estado;
    private final String observacion;

    @Builder.Default
    private final LocalDateTime fechaHora = LocalDateTime.now();
}
