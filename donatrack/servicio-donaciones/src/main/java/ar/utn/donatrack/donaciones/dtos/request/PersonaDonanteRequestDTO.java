package ar.utn.donatrack.donaciones.dtos.request;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "tipo")
@JsonSubTypes({
    @JsonSubTypes.Type(value = PersonaHumanaRequestDTO.class, name = "HUMANA"),
    @JsonSubTypes.Type(value = PersonaJuridicaRequestDTO.class, name = "JURIDICA")
})

@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
public abstract class PersonaDonanteRequestDTO {

  @NotBlank
  private String tipoDocumento;

  @NotBlank
  private String numeroDocumento;

  @NotBlank
  @Email
  private String email;

  @NotBlank
  private DireccionDTO direccion;

  @NotBlank
  private MedioDeContactoDTO medioContactoPredeterminado;

  @Builder.Default
  private List<MedioDeContactoDTO> contactos = new ArrayList<>();
}