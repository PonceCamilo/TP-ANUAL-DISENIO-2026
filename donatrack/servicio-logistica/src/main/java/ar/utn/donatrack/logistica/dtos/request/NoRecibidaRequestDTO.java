package ar.utn.donatrack.logistica.dtos.request;

import ar.utn.donatrack.logistica.models.entrega.MotivoFalloEntrega;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class NoRecibidaRequestDTO {
    // El chofer solo informa el motivo; Logística deriva si es replanificable.
    @NotNull
    private MotivoFalloEntrega motivo;
}
