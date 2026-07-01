package ar.utn.donatrack.logistica.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class NoRecibidaRequestDTO {
    @NotBlank
    private String motivo;
}
