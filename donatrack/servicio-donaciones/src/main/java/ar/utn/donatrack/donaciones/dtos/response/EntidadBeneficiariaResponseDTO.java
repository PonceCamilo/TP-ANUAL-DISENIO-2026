package ar.utn.donatrack.donaciones.dtos.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Builder
@Getter
public class EntidadBeneficiariaResponseDTO {
    private final UUID id;
    private final String razonSocial;
    private final DireccionResponseDTO direccion;
    private final List<MedioDeContactoResponseDTO> contactos;
    private final List<RepresentanteResponseDTO> representantes;
    private final List<CampaniaResponseDTO> campanias;
}