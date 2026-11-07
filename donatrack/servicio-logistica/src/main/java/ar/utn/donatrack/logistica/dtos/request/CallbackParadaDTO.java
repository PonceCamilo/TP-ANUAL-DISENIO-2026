package ar.utn.donatrack.logistica.dtos.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class CallbackParadaDTO {
    private int orden;
    @NotNull
    private UUID idEntidadBeneficiaria;
    @NotNull
    @Valid
    private DireccionRequestDTO direccion;
    @NotEmpty
    private List<UUID> donacionesIds;
}
