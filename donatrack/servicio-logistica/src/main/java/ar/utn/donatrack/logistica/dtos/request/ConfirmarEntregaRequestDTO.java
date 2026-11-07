package ar.utn.donatrack.logistica.dtos.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ConfirmarEntregaRequestDTO {
    @NotEmpty
    private List<String> fotosComprobante;
}
