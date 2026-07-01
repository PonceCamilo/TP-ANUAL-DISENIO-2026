package ar.utn.donatrack.logistica.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DireccionRequestDTO {
    @NotBlank
    private String calle;
    private int numero;
    @NotBlank
    private String localidad;
    @NotBlank
    private String provincia;
    private String codigoPostal;
}
