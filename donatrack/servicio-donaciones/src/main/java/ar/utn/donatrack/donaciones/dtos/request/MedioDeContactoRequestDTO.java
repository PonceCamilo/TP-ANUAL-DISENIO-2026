package ar.utn.donatrack.donaciones.dtos.request;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "tipo")
@JsonSubTypes({
    @JsonSubTypes.Type(value = EmailRequestDTO.class, name = "EMAIL"),
    @JsonSubTypes.Type(value = TelefonoRequestDTO.class, name = "TELEFONO"),
    @JsonSubTypes.Type(value = WhatsappRequestDTO.class, name = "WHATSAPP")
})

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class MedioDeContactoRequestDTO {

  @NotBlank
  protected String valor;
}