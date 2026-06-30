package ar.utn.donatrack.logistica.dtos.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class PlanificacionRequestDTO {
    @NotEmpty
    @Valid
    private List<DonacionParaRutearRequestDTO> donaciones;
    @NotEmpty
    private List<UUID> camionesIds;
}
