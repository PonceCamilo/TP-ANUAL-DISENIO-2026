package ar.utn.donatrack.donaciones.dtos.request;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "tipo")
@JsonSubTypes({
    @JsonSubTypes.Type(value = PersonaHumanaRequestDTO.class,  name = "HUMANA"),
    @JsonSubTypes.Type(value = PersonaJuridicaRequestDTO.class, name = "JURIDICA")
})
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class PersonaDonanteRequestDTO {

  @NotBlank
  private String tipoDocumento;

  @NotBlank
  private String numeroDocumento;

  @NotBlank
  @Email
  private String email;

  @NotNull
  @Valid
  private DireccionRequestDTO direccion;

  @NotNull
  @Valid
  private MedioDeContactoRequestDTO medioContactoPredeterminado;
}