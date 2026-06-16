package ar.utn.donatrack.donaciones.dtos.response;

import ar.utn.donatrack.donaciones.dtos.request.MedioDeContactoRequestDTO;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RepresentanteResponseDTO {

  private String nombre;
  private String apellido;
  private String email;
  private List<MedioDeContactoResponseDTO> contactos;
}
