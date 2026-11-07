package ar.utn.donatrack.logistica.models.entrega;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Renglón embebido en el historial de una entrega. No tiene id:
 * la API no lo identifica por separado.
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CambioEstadoEntrega {
    private EstadoEntrega estado;

    // Solo en los cambios a NO_RECIBIDA.
    private MotivoFalloEntrega motivoFallo;

    private String observacion;

    @Builder.Default
    private LocalDateTime fechaHora = LocalDateTime.now();
}
