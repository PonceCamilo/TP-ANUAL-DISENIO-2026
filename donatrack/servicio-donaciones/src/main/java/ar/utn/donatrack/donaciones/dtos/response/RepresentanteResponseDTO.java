package ar.utn.donatrack.donaciones.dtos.response;


import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RepresentanteResponseDTO {

  private final String nombre;
  private final String apellido;
  private final String email;
  private final List<MedioDeContactoResponseDTO> contactos;
}
