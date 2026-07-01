package ar.utn.donatrack.logistica.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CamionRequestDTO {
    @NotBlank
    private String patente;
    @Positive
    private double capacidadVolumenM3;
    @Positive
    private double alturaM;
    @Positive
    private double capacidadCargaKg;
}
