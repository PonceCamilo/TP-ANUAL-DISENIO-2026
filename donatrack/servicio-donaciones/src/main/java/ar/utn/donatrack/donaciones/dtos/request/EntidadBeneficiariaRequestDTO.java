package ar.utn.donatrack.donaciones.dtos.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EntidadBeneficiariaRequestDTO {

    @NotBlank(message = "La razón social no puede estar vacía")
    private String razonSocial;

    @NotNull(message = "La dirección es obligatoria")
    @Valid
    private DireccionRequestDTO direccion;

    @Valid
    private List<MedioDeContactoRequestDTO> contactos;

    @Valid
    private List<RepresentanteRequestDTO> representantes;
}