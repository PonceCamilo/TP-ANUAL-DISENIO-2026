package ar.utn.donatrack.donaciones.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EntidadBeneficiariaResponseDTO {
    private UUID id;
    private String razonSocial;
    private DireccionResponseDTO direccion;
    private List<MedioDeContactoResponseDTO> contactos;
    private List<RepresentanteResponseDTO> representantes;
    private List<CampaniaResponseDTO> campanias;
}