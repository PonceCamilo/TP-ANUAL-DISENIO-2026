package ar.utn.donatrack.donaciones.dtos.response;

import ar.utn.donatrack.donaciones.models.donante.EstadoDonante;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "tipo")
@JsonSubTypes({
    @JsonSubTypes.Type(value = PersonaHumanaResponseDTO.class,  name = "HUMANA"),
    @JsonSubTypes.Type(value = PersonaJuridicaResponseDTO.class, name = "JURIDICA")
})
@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public abstract class PersonaDonanteResponseDTO {

  private UUID id;
  private String tipoDocumento;
  private String numeroDocumento;
  private String email;
  private EstadoDonante estado;
  private MedioDeContactoResponseDTO medioContactoPredeterminado;
  private List<MedioDeContactoResponseDTO> contactos;
  private DireccionResponseDTO direccion;
}